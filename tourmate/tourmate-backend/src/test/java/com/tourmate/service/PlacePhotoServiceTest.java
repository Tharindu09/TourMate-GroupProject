package com.tourmate.service;
import com.tourmate.dto.PlacePhoto;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class PlacePhotoServiceTest {
    private HttpServer server;
    private PlacePhotoService photos;
    private final List<String> requests = new ArrayList<>();
    private final AtomicReference<String> media = new AtomicReference<>("{\"wikimedia_commons\":\"File:Specific venue.jpg\"}");
    private final AtomicReference<String> imageUrl = new AtomicReference<>("https://upload.wikimedia.org/wikipedia/commons/a/ab/Specific_venue.jpg");
    private final AtomicReference<String> license = new AtomicReference<>("CC BY-SA 4.0");
    private final AtomicInteger commonsStatus = new AtomicInteger(200);
    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String uri = exchange.getRequestURI().toString(); requests.add(uri);
            String body;
            int status = 200;
            if (uri.startsWith("/v2/place-details")) {
                body = "{\"features\":[{\"properties\":{\"feature_type\":\"details\",\"place_id\":\"canonical_venue_id\",\"name\":\"Never hardcoded venue\",\"lat\":8.5,\"lon\":81.2,\"categories\":[],\"wiki_and_media\":" + media.get() + "}}]}";
            } else if (uri.startsWith("/wiki/Special:EntityData/")) {
                body = "{\"entities\":{\"Q42\":{\"claims\":{\"P18\":[{\"mainsnak\":{\"datavalue\":{\"value\":\"Specific venue.jpg\"}}}]}}}}";
            } else if (uri.contains("pageimages")) {
                body = "{\"query\":{\"pages\":{\"1\":{\"original\":{\"source\":\"https://upload.wikimedia.org/wikipedia/commons/a/ab/Specific_venue.jpg\"}}}}}";
            } else {
                status = commonsStatus.get();
                body = "{\"query\":{\"pages\":{\"1\":{\"imageinfo\":[{\"mime\":\"image/jpeg\",\"thumburl\":\"" + imageUrl.get() + "\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:Specific_venue.jpg\",\"extmetadata\":{\"Artist\":{\"value\":\"<a>Example &amp; Author</a>\"},\"LicenseShortName\":{\"value\":\"" + license.get() + "\"},\"LicenseUrl\":{\"value\":\"https://creativecommons.org/licenses/by-sa/4.0/\"}}}]}}}}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) {stream.write(bytes);}
        });
        server.start(); String base = "http://127.0.0.1:" + server.getAddress().getPort();
        var places = new GeoapifyService(RestClient.builder(), "test-not-a-real-key", base);
        photos = new PlacePhotoService(places, RestClient.builder(), base, base, base);
    }
    @AfterEach void stop() {server.stop(0);}
    @Test void followsTheExactCommonsReferenceAndReturnsAttribution() {
        PlacePhoto photo = photos.photo("new_venue_id");
        assertEquals(imageUrl.get(), photo.url()); assertEquals("Example & Author", photo.author());
        assertEquals("CC BY-SA 4.0", photo.license()); assertEquals(2, requests.size());
        assertTrue(java.net.URLDecoder.decode(requests.getLast(), StandardCharsets.UTF_8).contains("Specific venue.jpg"));
    }
    @Test void resolvesWikidataP18ForAnArbitraryPlace() {
        media.set("{\"wikidata\":\"Q42\"}");
        assertNotNull(photos.photo("new_venue_id").url());
        assertTrue(requests.stream().anyMatch(path -> path.contains("EntityData/Q42.json")));
    }
    @Test void resolvesOnlyTheLinkedWikipediaArticle() {
        media.set("{\"wikipedia\":\"en:A specific venue\"}");
        assertNotNull(photos.photo("new_venue_id").url());
        assertTrue(requests.stream().anyMatch(path -> path.contains("pageimages")));
        assertFalse(requests.stream().anyMatch(path -> path.contains("geosearch")));
    }
    @Test void recognizesCommonsImageUrlsWithoutFetchingArbitraryHosts() {
        media.set("{\"image\":\"https://upload.wikimedia.org/wikipedia/commons/a/ab/Specific_venue.jpg\"}");
        assertNotNull(photos.photo("new_venue_id").url()); assertEquals(2, requests.size());
    }
    @Test void missingMediaReturnsNoPhotoAndCachesTheAbsence() {
        media.set("{}"); assertNull(photos.photo("new_venue_id").url());
        assertNull(photos.photo("new_venue_id").url()); assertEquals(1, requests.size());
    }
    @Test void cachesPhotoLookupsToAvoidRepeatedProviderCredits() {
        assertNotNull(photos.photo("new_venue_id").url());
        assertNotNull(photos.photo("new_venue_id").url()); assertEquals(2, requests.size());
    }
    @Test void doesNotUseGenericCategoriesOrUntrustedUrls() {
        media.set("{\"wikimedia_commons\":\"Category:Hotels\",\"image\":\"http://127.0.0.1/private\",\"wikipedia\":\"https://attacker.example/wiki/Test\",\"wikidata\":\"../secret\"}");
        assertNull(photos.photo("new_venue_id").url()); assertEquals(1, requests.size());
    }
    @Test void rejectsImagesWithoutLicensingMetadata() {
        license.set(""); assertNull(photos.photo("new_venue_id").url());
    }
    @Test void rejectsUnexpectedImageHosts() {
        imageUrl.set("https://attacker.example/image.jpg"); assertNull(photos.photo("new_venue_id").url());
    }
    @Test void transientFailuresCanBeRetriedWithoutRefetchingGeoapifyDetails() {
        commonsStatus.set(503);
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> photos.photo("new_venue_id")).getStatusCode().value());
        commonsStatus.set(200); assertNotNull(photos.photo("new_venue_id").url());
        assertEquals(1, requests.stream().filter(path -> path.startsWith("/v2/place-details")).count());
    }
}
