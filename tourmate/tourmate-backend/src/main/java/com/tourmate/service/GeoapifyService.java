package com.tourmate.service;

import com.tourmate.dto.LocationCenter;
import com.tourmate.dto.GeoMedia;
import com.tourmate.dto.GeoPlace;
import com.tourmate.dto.GeoSearchResult;
import com.tourmate.model.Destination;
import com.tourmate.model.Place;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

@Service
public class GeoapifyService {
    private static final int PAGE_SIZE = 12;
    private static final Map<String, String> CATEGORIES = Map.of(
        "", "accommodation,catering,tourism.attraction",
        "HOTEL", "accommodation.hotel,accommodation.guest_house",
        "RESTAURANT", "catering.restaurant,catering.cafe",
        "ATTRACTION", "tourism.attraction",
        "ACTIVITY", "leisure.park,natural,tourism.attraction");
    private final RestClient client;
    private final String apiKey;
    private record CachedPlace(GeoPlace place, long expiresAt) {}
    private final java.util.concurrent.ConcurrentHashMap<String, CachedPlace> detailsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private long windowStarted = System.nanoTime();
    private int requests;
    public GeoapifyService(RestClient.Builder builder,
        @Value("${app.geoapify.api-key:}") String apiKey,
        @Value("${app.geoapify.base-url:https://api.geoapify.com}") String baseUrl) {
        this.apiKey = apiKey.strip();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        client = builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
    private synchronized void checkAccess() {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Geoapify is not configured. Set GEOAPIFY_API_KEY in tourmate/.env and recreate the backend.");
        long now = System.nanoTime();
        if (now - windowStarted >= Duration.ofMinutes(1).toNanos()) {windowStarted = now; requests = 0;}
        if (++requests > 120) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            "Place search request limit reached. Please wait a minute.");
    }
    public GeoSearchResult search(String query, String type, String name, int radius, int page, Double latitude, Double longitude) {
        if (query == null || query.isBlank() || query.length() > 150) throw badRequest("Enter a town or destination (up to 150 characters)");
        if (!CATEGORIES.containsKey(type)) throw badRequest("Unsupported place category");
        if (name == null || name.length() > 120) throw badRequest("Place name must be at most 120 characters");
        if (radius < 1000 || radius > 50000 || page < 0 || page > 99) throw badRequest("Radius must be 1–50 km and page between 0 and 99");
        if ((latitude == null) != (longitude == null)) throw badRequest("Both latitude and longitude are required");
        String resolvedLocation = query.strip();
        if (latitude == null) {
            LocationCenter center = locate(query);
            latitude = center.latitude(); longitude = center.longitude(); resolvedLocation = center.name();
        }
        validateCoordinates(latitude, longitude);
        double lat = latitude; double lon = longitude;
        JsonNode response = get(builder -> {
            builder.path("/v2/places").queryParam("categories", CATEGORIES.get(type))
                .queryParam("filter", "circle:" + lon + "," + lat + "," + radius)
                .queryParam("bias", "proximity:" + lon + "," + lat)
                .queryParam("limit", PAGE_SIZE).queryParam("offset", page * PAGE_SIZE).queryParam("lang", "en");
            if (!name.isBlank()) builder.queryParam("name", name.strip());
            return builder.queryParam("apiKey", apiKey).build();
        });
        JsonNode features = response.path("features");
        if (!features.isArray()) throw unavailable();
        List<GeoPlace> places = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            if (usable(properties)) places.add(map(properties));
        }
        return new GeoSearchResult(places, page, features.size() == PAGE_SIZE && page < 99, lat, lon, resolvedLocation);
    }
    public LocationCenter locate(String query) {
        if (query == null || query.isBlank() || query.length() > 150) throw badRequest("Enter a destination (up to 150 characters)");
        JsonNode geocoded = get(builder -> builder.path("/v1/geocode/search").queryParam("text", "{query}")
            .queryParam("limit", 1).queryParam("format", "json")
            .queryParam("apiKey", apiKey).build(query.strip()));
        JsonNode results = geocoded.path("results");
        if (!results.isArray()) throw unavailable();
        if (results.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found. Try adding a region or country.");
        JsonNode center = results.get(0);
        if (!center.path("lat").isNumber() || !center.path("lon").isNumber()) throw unavailable();
        double lat = center.get("lat").asDouble(); double lon = center.get("lon").asDouble();
        validateCoordinates(lat, lon);
        return new LocationCenter(lat, lon, center.path("formatted").asText(query.strip()));
    }
    public GeoPlace detail(String id) {
        if (id == null || id.length() > 2048 || !id.matches("[A-Za-z0-9_-]+")) throw badRequest("Invalid Geoapify place ID");
        CachedPlace cached = detailsCache.get(id);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) return cached.place();
        JsonNode response = get(builder -> builder.path("/v2/place-details").queryParam("id", id)
            .queryParam("features", "details").queryParam("lang", "en").queryParam("apiKey", apiKey).build());
        JsonNode features = response.path("features");
        if (!features.isArray()) throw unavailable();
        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            // Geoapify may return a canonical ID different from its search-result ID.
            // Select the primary details feature, never a nearby-place feature.
            String featureType = properties.path("feature_type").asText();
            boolean primary = "details".equals(featureType) ||
                (featureType.isBlank() && id.equals(properties.path("place_id").asText()));
            if (usable(properties) && primary) {
                GeoPlace place = map(properties, id);
                if (detailsCache.size() >= 512) detailsCache.clear();
                detailsCache.put(id, new CachedPlace(place, System.currentTimeMillis() + Duration.ofMinutes(15).toMillis()));
                return place;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Place details are unavailable");
    }
    private JsonNode get(Function<UriBuilder, java.net.URI> uri) {
        checkAccess();
        try {
            JsonNode response = client.get().uri(uri).retrieve().body(JsonNode.class);
            if (response == null) throw unavailable();
            return response;
        } catch (RestClientResponseException ex) {
            throw switch (ex.getStatusCode().value()) {
                case 400 -> badRequest("Geoapify could not process this search. Try a different destination or place name.");
                case 401, 403 -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geoapify rejected the backend API key. Check the key and its restrictions.");
                case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found");
                case 429 -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Geoapify quota reached. Please try later.");
                default -> unavailable();
            };
        } catch (RestClientException ex) {throw unavailable();}
        catch (IllegalArgumentException ex) {throw badRequest("Unsupported search text or identifier");}
    }
    private boolean usable(JsonNode properties) {
        double lat = properties.path("lat").asDouble(Double.NaN), lon = properties.path("lon").asDouble(Double.NaN);
        return !properties.path("place_id").asText().isBlank() && properties.path("lat").isNumber() && properties.path("lon").isNumber()
            && Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
    private GeoPlace map(JsonNode p) {return map(p, p.path("place_id").asText());}
    private GeoPlace map(JsonNode p, String requestedId) {
        List<String> categories = new ArrayList<>();
        p.path("categories").forEach(category -> {if (category.isTextual()) categories.add(category.asText());});
        return new GeoPlace(requestedId, p.path("name").asText(p.path("address_line1").asText("Unnamed place")),
            p.path("formatted").asText("Address unavailable"), p.path("lat").asDouble(), p.path("lon").asDouble(), categories,
            text(p, "website"), text(p.path("contact"), "phone"), text(p, "opening_hours"), text(p, "description"),
            text(p, "wheelchair"), new GeoMedia(text(p.path("wiki_and_media"), "image"),
                text(p.path("wiki_and_media"), "wikimedia_commons"), text(p.path("wiki_and_media"), "wikidata"),
                text(p.path("wiki_and_media"), "wikipedia")));
    }
    private String text(JsonNode node, String field) {return node.path(field).isValueNode() && !node.path(field).isNull() ? node.get(field).asText() : null;}
    private void validateCoordinates(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw badRequest("Provide valid latitude and longitude.");
    }
    private ResponseStatusException badRequest(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);}
    private ResponseStatusException unavailable() {return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Geoapify is unavailable. Please try again later.");}
}
