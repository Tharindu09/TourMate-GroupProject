package com.tourmate.service;

import com.tourmate.dto.MapLocation;
import com.tourmate.dto.TravelPlace;
import com.tourmate.dto.TravelSearchResult;
import com.tourmate.model.Place;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

@Service
public class RapidTravelService {
    private static final Map<String, String> TYPES = Map.of("ATTRACTION", "attractions", "ACTIVITY", "attractions", "RESTAURANT", "restaurants", "HOTEL", "hotels", "BOOKING_HOTEL", "hotels");
    private final GeoapifyService locations;
    private final RestClient travel;
    private final RestClient booking;
    private final String apiKey;
    private final String travelHost;
    private final String bookingHost;
    private record Context(String type, LocalDate checkin, LocalDate checkout, int rooms, int adults, long expiresAt, Map<String, TravelPlace> results) {}
    private final ConcurrentHashMap<String, Context> contexts = new ConcurrentHashMap<>();
    private long minuteStarted = System.nanoTime();
    private int requests;
    public RapidTravelService(GeoapifyService locations, RestClient.Builder builder,
        @Value("${app.rapidapi.api-key:}") String apiKey,
        @Value("${app.rapidapi.travel-host:travel-advisor.p.rapidapi.com}") String travelHost,
        @Value("${app.rapidapi.booking-host:booking-com18.p.rapidapi.com}") String bookingHost,
        @Value("${app.rapidapi.travel-base-url:}") String travelBase,
        @Value("${app.rapidapi.booking-base-url:}") String bookingBase) {
        this.locations = locations; this.apiKey = apiKey.strip(); this.travelHost = travelHost; this.bookingHost = bookingHost;
        if (!travelHost.matches("[A-Za-z0-9.-]+") || !bookingHost.matches("[A-Za-z0-9.-]+")) throw new IllegalArgumentException("Invalid RapidAPI host");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        travel = builder.clone().requestFactory(factory).baseUrl(travelBase.isBlank() ? "https://" + travelHost : travelBase).build();
        booking = builder.clone().requestFactory(factory).baseUrl(bookingBase.isBlank() ? "https://" + bookingHost : bookingBase).build();
    }
    private void configured() {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RapidAPI is not configured. Set RAPIDAPI_KEY in tourmate/.env and recreate the backend.");
    }
    private synchronized void permit() {
        configured(); long now = System.nanoTime();
        if (now - minuteStarted >= Duration.ofMinutes(1).toNanos()) {minuteStarted = now; requests = 0;}
        if (++requests > 30) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Travel provider request limit reached. Please wait a minute.");
    }
    public TravelSearchResult search(String query, String type, int radius, int page, Double latitude, Double longitude,
        LocalDate checkin, LocalDate checkout, int rooms, int adults) {
        validateType(type); validateStay(type, checkin, checkout, rooms, adults);
        if (query == null || query.isBlank() || query.length() > 150 || radius < 1000 || radius > 50000 || page < 0 || page > 99)
            throw bad("Enter a destination, radius of 1–50 km and page between 0 and 99");
        if ((latitude == null) != (longitude == null)) throw bad("Both coordinates must be provided together");
        configured();
        if (usesV2List(type)) {
            if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
                throw bad("Provide valid latitude and longitude.");
            return searchV2Places(query.strip(), type, radius, page, latitude, longitude);
        }
        String location = query.strip();
        if (latitude == null) {
            var center = locations.locate(query); latitude = center.latitude(); longitude = center.longitude(); location = center.name();
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw bad("Provide valid latitude and longitude.");
        double lat = latitude; double lon = longitude;
        double dlat = radius / 111320.0; double dlon = Math.min(180, dlat / Math.max(0.000001, Math.abs(Math.cos(Math.toRadians(lat)))));
        boolean bookingSearch = "BOOKING_HOTEL".equals(type);
        if (bookingSearch && page != 0) throw bad("This Booking search endpoint does not expose verified pagination");
        JsonNode response = request(bookingSearch, b -> {
            b.path(bookingSearch ? "/stays/search-by-geo" : "/" + TYPES.get(type) + "/list-in-boundary");
            if (bookingSearch) {
                b.queryParam("neLat", Math.min(90, lat + dlat)).queryParam("neLng", Math.min(180, lon + dlon))
                    .queryParam("swLat", Math.max(-90, lat - dlat)).queryParam("swLng", Math.max(-180, lon - dlon))
                    .queryParam("checkinDate", checkin).queryParam("checkoutDate", checkout)
                    .queryParam("rooms", rooms).queryParam("adults", adults).queryParam("units", "metric");
            } else {
                b.queryParam("bl_latitude", Math.max(-90, lat - dlat)).queryParam("bl_longitude", Math.max(-180, lon - dlon))
                    .queryParam("tr_latitude", Math.min(90, lat + dlat)).queryParam("tr_longitude", Math.min(180, lon + dlon))
                    .queryParam("limit", 12).queryParam("offset", page * 12).queryParam("lang", "en_US").queryParam("currency", "USD");
                if ("HOTEL".equals(type)) stayParams(b, checkin, checkout, rooms, adults);
            }
            return b.build();
        });
        JsonNode data = bookingSearch ? response.path("data").path("results") : response.path("data");
        if (!data.isArray()) throw unavailable("The travel provider returned an unsupported response. Check the subscribed API endpoint and plan.");
        String token = UUID.randomUUID().toString();
        Map<String, TravelPlace> mapped = new LinkedHashMap<>();
        for (JsonNode raw : data) {
            TravelPlace place = bookingSearch ? mapBooking(raw, token) : mapTravel(raw, type, token);
            if (place != null) mapped.put(place.id(), place);
        }
        if (!data.isEmpty() && mapped.isEmpty()) throw unavailable("The provider returned listings without supported identifiers. Its API response format may have changed.");
        if (contexts.size() >= 300) contexts.clear();
        contexts.put(token, new Context(type, checkin, checkout, rooms, adults, System.currentTimeMillis() + Duration.ofMinutes(15).toMillis(), mapped));
        return new TravelSearchResult(List.copyOf(mapped.values()), page, !bookingSearch && data.size() == 12 && page < 99, lat, lon, location);
    }
    public TravelPlace detail(String type, String id, String token) {
        validateType(type);
        if (id == null || !id.matches("[0-9]{1,30}")) throw bad("Invalid provider place ID");
        configured();
        Context context = token == null ? null : contexts.get(token);
        if (context != null && (context.expiresAt() <= System.currentTimeMillis() || !type.equals(context.type()))) context = null;
        TravelPlace summary = context == null ? null : context.results().get(id);
        if ("BOOKING_HOTEL".equals(type) || usesV2List(type)) {
            if (summary == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search result expired. Search again before opening this place.");
            return summary;
        }
        if ("HOTEL".equals(type) && context == null) throw bad("Search hotels with your dates before opening the details");
        Context stay = context;
        JsonNode response = request(false, b -> {
            b.path("/" + TYPES.get(type) + "/get-details").queryParam("location_id", id).queryParam("lang", "en_US").queryParam("currency", "USD");
            if ("HOTEL".equals(type)) stayParams(b, stay.checkin(), stay.checkout(), stay.rooms(), stay.adults());
            return b.build();
        });
        JsonNode data = response.path("data");
        if (data.isArray()) {
            JsonNode matched = null;
            for (JsonNode item : data) if (id.equals(item.path("location_id").asText())) {matched = item; break;}
            data = matched;
        }
        if (data == null || !data.isObject() || !id.equals(data.path("location_id").asText()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The provider did not return matching place details");
        // Merge only the same provider ID, retaining fields from this search if details omit them.
        TravelPlace result = mapTravel(data, type, token);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Place details unavailable");
        return merge(result, summary);
    }
    public MapLocation mapLocation(String type, String id, String token) {
        validateType(type);
        Context context = token == null ? null : contexts.get(token);
        if (context == null || context.expiresAt() <= System.currentTimeMillis() || !type.equals(context.type()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search result expired. Search again before viewing the map.");
        TravelPlace place = context.results().get(id);
        if (place == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found in this search");
        if (place.latitude() != null && place.longitude() != null) return new MapLocation(place.latitude(), place.longitude());
        if (!usesV2List(type)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The provider has not supplied coordinates for this place.");
        return resolveCoordinates(id,place.name());
    }
    public MapLocation resolveCoordinates(String id,String name) {
        JsonNode response = request(false, b -> b.path("/locations/v2/auto-complete")
            .queryParam("query", "{query}").queryParam("lang", "en_US").queryParam("units", "km").build(name));
        for (JsonNode candidate : response.path("data").path("Typeahead_autocomplete").path("results")) {
            JsonNode info = candidate.path("detailsV2");
            if (!id.equals(info.path("locationId").asText())) continue;
            Double lat = coordinate(info.path("geocode").path("latitude"), -90, 90);
            Double lon = coordinate(info.path("geocode").path("longitude"), -180, 180);
            if (lat != null && lon != null) return new MapLocation(lat, lon);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The provider has not supplied coordinates for this place. Try searching OpenStreetMap by name.");
    }

    private boolean usesV2List(String type) {return "ATTRACTION".equals(type) || "ACTIVITY".equals(type) || "RESTAURANT".equals(type);}

    private TravelSearchResult searchV2Places(String query, String type, int radius, int page, Double latitude, Double longitude) {
        if (page != 0) throw bad("This travel search does not expose verified pagination");
        JsonNode autocomplete = request(false, b -> b.path("/locations/v2/auto-complete")
            .queryParam("query", "{query}").queryParam("lang", "en_US").queryParam("units", "km").build(query));
        JsonNode candidates = autocomplete.path("data").path("Typeahead_autocomplete").path("results");
        Long geoId = null;
        double closest = Double.POSITIVE_INFINITY;
        Double resolvedLat = latitude; Double resolvedLon = longitude;
        String resolvedName = query;
        for (JsonNode candidate : candidates) {
            JsonNode info = candidate.path("detailsV2");
            Double candidateLat = coordinate(info.path("geocode").path("latitude"), -90, 90);
            Double candidateLon = coordinate(info.path("geocode").path("longitude"), -180, 180);
            JsonNode id = info.path("locationId");
            if (!info.path("isGeo").asBoolean() || !id.isIntegralNumber() || id.asLong() <= 0 || candidateLat == null || candidateLon == null) continue;
            double distance = latitude == null ? 0 : Math.pow(candidateLat - latitude, 2) + Math.pow(candidateLon - longitude, 2);
            if (distance < closest) {
                closest = distance; geoId = id.asLong();
                if (latitude == null) {resolvedLat = candidateLat; resolvedLon = candidateLon;}
                String name = value(info.path("names"), "name");
                if (name != null) resolvedName = name;
            }
        }
        if (geoId == null || closest > 0.25 * 0.25)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel Advisor could not match this destination. Try adding its region or country.");
        double lat = resolvedLat; double lon = resolvedLon;
        double dlat = radius / 111320.0; double dlon = Math.min(180, dlat / Math.max(0.000001, Math.abs(Math.cos(Math.toRadians(lat)))));
        boolean restaurant = "RESTAURANT".equals(type);
        Map<String, Object> body = Map.of("geoId", geoId,
            "pax", List.of(Map.of("ageBand", "ADULT", "count", 2)),
            "sort", restaurant ? "POPULARITY" : "TRAVELER_FAVORITE_V2", "sortOrder", restaurant ? "desc" : "asc", "filters", List.of(), "updateToken", "",
            "boundingBox", Map.of("northEastCorner", Map.of("latitude", Math.min(90, lat + dlat), "longitude", Math.min(180, lon + dlon)),
                "southWestCorner", Map.of("latitude", Math.max(-90, lat - dlat), "longitude", Math.max(-180, lon - dlon))));
        JsonNode response = request(false, b -> b.path("/" + TYPES.get(type) + "/v2/list")
            .queryParam("currency", "USD").queryParam("units", "km").queryParam("lang", "en_US").build(), body);
        JsonNode lists = response.path("data").path("AppPresentation_queryAppListV2");
        JsonNode sections = lists.path(0).path("sections");
        if (!sections.isArray()) throw unavailable("Travel Advisor returned an unsupported place response");
        String token = UUID.randomUUID().toString();
        Map<String, TravelPlace> mapped = new LinkedHashMap<>();
        for (JsonNode section : sections) {
            TravelPlace place = mapV2Place(section.path("listSingleCardContent"), type, token);
            if (place != null) mapped.put(place.id(), place);
        }
        if (contexts.size() >= 300) contexts.clear();
        contexts.put(token, new Context(type, null, null, 1, 2, System.currentTimeMillis() + Duration.ofMinutes(15).toMillis(), mapped));
        return new TravelSearchResult(List.copyOf(mapped.values()), 0, false, lat, lon, resolvedName);
    }

    private TravelPlace mapV2Place(JsonNode card, String type, String token) {
        String id = value(card.path("saveId"), "id");
        String name = value(card.path("cardTitle"), "string");
        if (id == null || !id.matches("[0-9]{1,30}") || name == null) return null;
        name = name.replaceFirst("^\\d+\\.\\s*", "");
        String photo = value(card.path("cardPhoto").path("sizes"), "urlTemplate");
        if (photo != null) photo = photo.replace("{width}", "800").replace("{height}", "600");
        Double rating = coordinate(card.path("bubbleRating").path("rating"), 0, 5);
        String reviewText = value(card.path("bubbleRating").path("numberReviews"), "string");
        Integer reviews = null;
        if (reviewText != null) {
            String digits = reviewText.replaceAll("[(),\\s]", "");
            try {reviews = Integer.valueOf(digits);} catch (NumberFormatException ignored) {}
        }
        String route = value(card.path("cardLink").path("route"), "url");
        String source = route != null && route.startsWith("/") && !route.startsWith("//") ? "https://www.tripadvisor.com" + route : null;
        return new TravelPlace(id, type, "Travel Advisor via RapidAPI", name, value(card.path("primaryInfo"), "text"),
            null, null, null, web(photo, true), "Photo supplied by Travel Advisor via RapidAPI", rating, 5,
            reviews, null, null, web(source, false),
            List.of(), List.of(), List.of(), null, "USD", token);
    }

    private TravelPlace merge(TravelPlace detail, TravelPlace summary) {
        if (summary == null) return detail;
        return new TravelPlace(detail.id(), detail.type(), detail.provider(), detail.name(), detail.description(),
            detail.address() == null ? summary.address() : detail.address(), detail.latitude() == null ? summary.latitude() : detail.latitude(),
            detail.longitude() == null ? summary.longitude() : detail.longitude(), detail.photoUrl() == null ? summary.photoUrl() : detail.photoUrl(),
            detail.photoCredit(), detail.rating() == null ? summary.rating() : detail.rating(), detail.ratingScale(), detail.reviewCount() == null ? summary.reviewCount() : detail.reviewCount(),
            detail.phone() == null ? summary.phone() : detail.phone(), detail.website() == null ? summary.website() : detail.website(),
            detail.sourceUrl() == null ? summary.sourceUrl() : detail.sourceUrl(), detail.openingHours(), detail.amenities(), detail.reviews(),
            detail.priceText() == null ? summary.priceText() : detail.priceText(), detail.currency(), detail.resultToken());
    }
    private void stayParams(UriBuilder b, LocalDate in, LocalDate out, int rooms, int adults) {
        b.queryParam("checkin", in).queryParam("nights", ChronoUnit.DAYS.between(in, out)).queryParam("rooms", rooms).queryParam("adults", adults);
    }
    private JsonNode request(boolean hotel, Function<UriBuilder, URI> uri) {
        return request(hotel, uri, null);
    }
    private JsonNode request(boolean hotel, Function<UriBuilder, URI> uri, Object body) {
        permit();
        try {
            RestClient client = hotel ? booking : travel;
            RestClient.RequestHeadersSpec<?> spec = body == null ? client.get().uri(uri)
                : client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body);
            JsonNode response = spec.header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", hotel ? bookingHost : travelHost).retrieve().body(JsonNode.class);
            if (response == null) throw unavailable("Travel provider returned no data");
            for (JsonNode error : response.path("errors")) {
                if ("NotFoundException".equals(error.path("type").asText()))
                    throw unavailable("The selected travel provider endpoint is unavailable. Its API integration needs to be updated.");
            }
            if (response.has("error") || response.has("errors") || (response.has("message") && !response.has("data")))
                throw unavailable("Travel provider could not complete this request. Check the API subscription and parameters.");
            return response;
        } catch (RestClientResponseException ex) {
            throw switch (ex.getStatusCode().value()) {
                case 401, 403 -> unavailable("RapidAPI rejected access. Check RAPIDAPI_KEY and subscribe to the specific API product.");
                case 429 -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "RapidAPI quota or rate limit reached. Check your provider plan.");
                case 400 -> bad("Travel provider rejected the search parameters. Check your dates and destination.");
                case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel provider endpoint or place not found. Check the selected API product.");
                default -> unavailable("Travel provider is temporarily unavailable");
            };
        } catch (RestClientException | IllegalArgumentException ex) {throw unavailable("Could not reach the travel provider. Please try again later.");}
    }
    private void validateType(String type) {if (type == null || !TYPES.containsKey(type)) throw bad("Unsupported travel category");}
    private void validateStay(String type, LocalDate in, LocalDate out, int rooms, int adults) {
        if (!type.endsWith("HOTEL")) return;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (in == null || out == null || in.isBefore(today) || !out.isAfter(in) || ChronoUnit.DAYS.between(in, out) > 30 || rooms < 1 || rooms > 8 || adults < rooms || adults > 30)
            throw bad("Provide future hotel dates (1–30 nights), 1–8 rooms and at least one adult per room (up to 30)");
    }
    private TravelPlace mapTravel(JsonNode p, String type, String token) {
        String id = value(p, "location_id"); String name = value(p, "name");
        if (id == null || !id.matches("[0-9]{1,30}") || name == null) return null;
        List<String> hours = strings(p.path("hours").path("weekday_text"));
        if (hours.isEmpty()) hours = strings(p.path("opening_hours").path("weekday_text"));
        List<TravelPlace.Review> reviews = new ArrayList<>();
        if (p.path("reviews").isArray()) for (JsonNode review : p.get("reviews")) {
            String text = value(review, "text");
            if (text != null) reviews.add(new TravelPlace.Review(value(review, "title"), text, value(review.path("user"), "username"), number(review.path("rating")), value(review, "published_date"), web(value(review, "url"), false)));
        }
        Double rating = number(p.path("rating")); if (rating != null && (rating < 0 || rating > 5)) rating = null;
        return new TravelPlace(id, type, "Travel Advisor via RapidAPI", name, value(p, "description"), value(p, "address"),
            coordinate(p.path("latitude"), -90, 90), coordinate(p.path("longitude"), -180, 180), web(value(p.path("photo").path("images").path("large"), "url"), true),
            "Photo supplied by Travel Advisor via RapidAPI", rating, 5, count(p.path("num_reviews")), value(p, "phone"),
            web(value(p, "website"), false), web(value(p, "web_url"), false), hours, labels(p.path("amenities")), reviews, value(p, "price"), value(p, "currency") == null ? "USD" : value(p, "currency"), token);
    }
    private TravelPlace mapBooking(JsonNode raw, String token) {
        JsonNode p = raw.path("basicPropertyData"); String id = value(p, "id"); String name = value(p, "name");
        if (id == null) id = value(raw, "id");
        if (id == null || !id.matches("[0-9]{1,30}") || name == null) return null;
        String photo = value(p.path("photos").path("main").path("highResJpegUrl"), "absoluteUrl");
        if (photo == null) photo = value(p.path("photos").path("main").path("lowResJpegUrl"), "absoluteUrl");
        JsonNode amount = raw.path("priceDisplayInfo").path("displayPrice").path("amountPerStay");
        String price = value(amount, "amountUnformatted");
        String currency = value(amount, "currency");
        if (currency == null) currency = value(raw.path("priceDisplayInfo"), "currency");
        Double rating = number(p.path("reviews").path("totalScore")); if (rating != null && (rating < 0 || rating > 10)) rating = null;
        return new TravelPlace(id, "BOOKING_HOTEL", "Booking COM via RapidAPI", name, null,
            value(p.path("location"), "city"), coordinate(p.path("location").path("latitude"), -90, 90), coordinate(p.path("location").path("longitude"), -180, 180),
            web(photo, true), "Photo supplied by Booking COM via RapidAPI", rating, 10, count(p.path("reviews").path("reviewsCount")),
            null, null, web(value(raw, "url"), false), List.of(), List.of(), List.of(), price, currency, token);
    }
    private List<String> labels(JsonNode values) {
        List<String> result = new ArrayList<>();
        if (values.isArray()) for (JsonNode item : values) {String label = item.isTextual() ? item.asText() : value(item, "name"); if (label != null && !label.isBlank()) result.add(label);}
        return result;
    }
    private List<String> strings(JsonNode values) {return labels(values);}
    private Double number(JsonNode value) {try {double n = Double.parseDouble(value.asText().replace(",", "")); return Double.isFinite(n) ? n : null;} catch (NumberFormatException ex) {return null;}}
    private Double coordinate(JsonNode value, double min, double max) {Double n = number(value);return n != null && n >= min && n <= max ? n : null;}
    private Integer count(JsonNode value) {Double n = number(value);return n != null && n >= 0 && n <= Integer.MAX_VALUE && n == Math.floor(n) ? n.intValue() : null;}
    private String value(JsonNode node, String key) {JsonNode v = node.path(key);return v.isValueNode() && !v.isNull() && !v.asText().isBlank() ? v.asText() : null;}
    private String web(String value, boolean image) {
        if (value == null) return null;
        try {URI uri = URI.create(value.startsWith("//") ? "https:" + value : value);if (uri.getHost() != null && ("https".equals(uri.getScheme()) || (!image && "http".equals(uri.getScheme())))) return uri.toString();}
        catch (IllegalArgumentException ignored) {} return null;
    }
    private ResponseStatusException bad(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);}
    private ResponseStatusException unavailable(String message) {return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);}
}
