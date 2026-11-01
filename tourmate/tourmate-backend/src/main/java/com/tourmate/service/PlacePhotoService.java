package com.tourmate.service;

import com.tourmate.dto.GeoMedia;
import com.tourmate.dto.PlacePhoto;
import com.tourmate.model.User;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriBuilder;

@Service
public class PlacePhotoService {
    private final GeoapifyService places;
    private final RestClient commons;
    private final RestClient wikidata;
    private final RestClient.Builder wikipedia;
    private final String wikipediaBase;
    private record CachedPhoto(PlacePhoto photo, long expiresAt) {}
    private final ConcurrentHashMap<String, CachedPhoto> cache = new ConcurrentHashMap<>();
    private final Semaphore concurrentLookups = new Semaphore(3);

    public PlacePhotoService(GeoapifyService places, RestClient.Builder builder,
        @Value("${app.photos.commons-base-url:https://commons.wikimedia.org}") String commonsBase,
        @Value("${app.photos.wikidata-base-url:https://www.wikidata.org}") String wikidataBase,
        @Value("${app.photos.wikipedia-base-url:}") String wikipediaBase) {
        this.places = places; this.wikipediaBase = wikipediaBase;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        var configured = builder.clone().requestFactory(factory).defaultHeader("User-Agent", "TourMate/0.1 (place-photo lookup; Wikimedia attribution displayed)");
        commons = configured.clone().baseUrl(commonsBase).build();
        wikidata = configured.clone().baseUrl(wikidataBase).build();
        wikipedia = configured;
    }
    public PlacePhoto photo(String id) {
        CachedPhoto existing = cache.get(id);
        if (existing != null && existing.expiresAt() > System.currentTimeMillis()) return existing.photo();
        if (!concurrentLookups.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Photo lookup is busy. Please retry shortly.");
        try {
            PlacePhoto photo = resolve(places.detail(id).media());
            if (cache.size() >= 512) cache.clear();
            cache.put(id, new CachedPhoto(photo, System.currentTimeMillis() + Duration.ofMinutes(30).toMillis()));
            return photo;
        } catch (RestClientException | IllegalArgumentException ex) {
            // No raw upstream URL, source metadata or API key is exposed through an exception.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Photo provider is unavailable. Please try again.");
        } finally {concurrentLookups.release();}
    }
    private PlacePhoto resolve(GeoMedia media) {
        if (media == null) return PlacePhoto.unavailable();
        Set<String> attempted = new HashSet<>();
        for (String candidate : Arrays.asList(fileTitle(media.commons()), fileTitle(media.image()))) {
            PlacePhoto photo = fromFile(candidate, attempted);
            if (photo != null) return photo;
        }
        String qid = entityId(media.wikidata());
        if (qid != null) {
            JsonNode entity = get(wikidata, b -> b.path("/wiki/Special:EntityData/{id}.json").build(qid));
            JsonNode claims = entity.path("entities").path(qid).path("claims").path("P18");
            if (claims.isArray()) for (JsonNode claim : claims) {
                String title = claim.path("mainsnak").path("datavalue").path("value").asText(null);
                PlacePhoto photo = fromFile(title == null ? null : "File:" + title, attempted);
                if (photo != null) return photo;
            }
        }
        String[] article = articleReference(media.wikipedia());
        if (article != null) {
            String base = wikipediaBase.isBlank() ? "https://" + article[0] + ".wikipedia.org" : wikipediaBase;
            JsonNode response = get(wikipedia.clone().baseUrl(base).build(), b -> b.path("/w/api.php")
                .queryParam("action", "query").queryParam("format", "json").queryParam("redirects", 1)
                .queryParam("prop", "pageimages").queryParam("piprop", "original")
                .queryParam("titles", "{title}").build(article[1]));
            for (JsonNode page : response.path("query").path("pages")) {
                PlacePhoto photo = fromFile(fileTitle(page.path("original").path("source").asText(null)), attempted);
                if (photo != null) return photo;
            }
        }
        return PlacePhoto.unavailable();
    }
    private PlacePhoto fromFile(String title, Set<String> attempted) {
        if (title == null || title.length() > 512 || !attempted.add(title)) return null;
        JsonNode response = get(commons, b -> b.path("/w/api.php").queryParam("action", "query").queryParam("format", "json")
            .queryParam("prop", "imageinfo").queryParam("iiprop", "url|extmetadata|mime").queryParam("iiurlwidth", 960)
            .queryParam("titles", "{title}").build(title));
        for (JsonNode page : response.path("query").path("pages")) {
            JsonNode info = page.path("imageinfo").path(0);
            String mime = info.path("mime").asText();
            if (!Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(mime)) continue;
            String url = info.path("thumburl").asText(info.path("url").asText());
            String source = info.path("descriptionurl").asText();
            JsonNode metadata = info.path("extmetadata");
            String author = clean(metadata.path("Artist").path("value").asText());
            String license = clean(metadata.path("LicenseShortName").path("value").asText());
            if (!imageUrl(url) || !commonsPage(source) || license.isBlank()) continue;
            String licenseUrl = metadata.path("LicenseUrl").path("value").asText(null);
            if (!webUrl(licenseUrl)) licenseUrl = null;
            return new PlacePhoto(url, source, author.isBlank() ? "See original source for author" : author, license, licenseUrl);
        }
        return null;
    }
    private JsonNode get(RestClient client, Function<UriBuilder, URI> uri) {
        try {
            JsonNode response = client.get().uri(uri).retrieve().body(JsonNode.class);
            if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Photo provider returned no data");
            if (response.has("error")) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Photo provider could not complete the lookup");
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            throw ex;
        }
    }
    private String fileTitle(String reference) {
        if (reference == null || reference.isBlank() || reference.length() > 2048) return null;
        String value = reference.strip();
        if (value.regionMatches(true, 0, "File:", 0, 5)) return "File:" + value.substring(5);
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme())) return null;
            String host = uri.getHost(); String path = uri.getPath();
            if ("commons.wikimedia.org".equals(host)) {
                if (path.startsWith("/wiki/File:")) return path.substring("/wiki/".length());
                if (path.startsWith("/wiki/Special:FilePath/")) return "File:" + path.substring("/wiki/Special:FilePath/".length());
            }
            if (("upload.wikimedia.org".equals(host) || "thumb.wikimedia.org".equals(host)) && path.startsWith("/wikipedia/commons/")) {
                String[] parts = path.split("/");
                return "File:" + parts[parts.length - (path.contains("/thumb/") ? 2 : 1)];
            }
        } catch (IllegalArgumentException ignored) { }
        return null;
    }
    private String entityId(String reference) {
        if (reference == null) return null;
        if (reference.matches("Q[1-9][0-9]{0,15}")) return reference;
        if (reference.matches("https://www[.]wikidata[.]org/(wiki|entity)/Q[1-9][0-9]{0,15}")) return reference.substring(reference.lastIndexOf('/') + 1);
        return null;
    }
    private String[] articleReference(String reference) {
        if (reference == null || reference.length() > 512) return null;
        if (!reference.contains("://") && reference.matches("[a-z]{2,12}:[^|#{}]+")) {
            int colon = reference.indexOf(':'); return new String[]{reference.substring(0, colon), reference.substring(colon + 1)};
        }
        try {
            URI uri = URI.create(reference); String host = uri.getHost();
            if ("https".equals(uri.getScheme()) && host != null && host.matches("[a-z]{2,12}[.]wikipedia[.]org") && uri.getPath().startsWith("/wiki/"))
                return new String[]{host.substring(0, host.indexOf('.')), uri.getPath().substring(6)};
        } catch (IllegalArgumentException ignored) { }
        return null;
    }
    private boolean imageUrl(String value) {
        try {URI uri = URI.create(value);return "https".equals(uri.getScheme()) && Set.of("upload.wikimedia.org", "thumb.wikimedia.org").contains(uri.getHost());}
        catch (IllegalArgumentException | NullPointerException ex) {return false;}
    }
    private boolean commonsPage(String value) {
        try {URI uri = URI.create(value);return "https".equals(uri.getScheme()) && "commons.wikimedia.org".equals(uri.getHost()) && uri.getPath().startsWith("/wiki/File:");}
        catch (IllegalArgumentException ex) {return false;}
    }
    private boolean webUrl(String value) {
        if (value == null) return false;
        try {URI uri = URI.create(value);return Set.of("https", "http").contains(uri.getScheme()) && uri.getHost() != null;}
        catch (IllegalArgumentException | NullPointerException ex) {return false;}
    }
    private String clean(String value) {return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).strip();}
}
