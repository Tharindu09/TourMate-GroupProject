package com.tourmate.service;
import com.tourmate.dto.GeoSearchResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class GeoapifyServiceTest {
    private HttpServer server;
    private GeoapifyService service;
    private final List<String> paths = new ArrayList<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> geocode = new AtomicReference<>("{\"results\":[{\"lat\":6.8667,\"lon\":81.0466,\"formatted\":\"Ella, Sri Lanka\"}]}");
    private final AtomicReference<String> places = new AtomicReference<>("""
        {"features":[{"properties":{"place_id":"place_1","name":"Test Cafe","formatted":"Ella","lat":6.87,"lon":81.04,"country_code":"lk","categories":["catering.cafe"],"website":"https://example.org","contact":{"phone":"0123456789"},"opening_hours":"Mo-Fr 09:00-17:00","datasource":{"apiKey":"must-not-leak"}}}]}
        """);
    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            synchronized(paths) { paths.add(exchange.getRequestURI().toString()); }
            String body = exchange.getRequestURI().getPath().contains("geocode") ? geocode.get() : places.get();
            if (status.get() != 200) body = "{\"error\":\"test-private-key should never reach the browser\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var stream = exchange.getResponseBody()) {stream.write(bytes);}
        });
        server.start(); service = new GeoapifyService(RestClient.builder(), "test-private-key", baseUrl());
    }
    @AfterEach void stop() {server.stop(0);}
    private String baseUrl() {return "http://127.0.0.1:" + server.getAddress().getPort();}
    private GeoSearchResult search() {return service.search("Ella", "RESTAURANT", "", 10000, 0, null, null);}
    @Test void geocodesDestinationThenQueriesCategoriesAndNormalizesResult() throws Exception {
        var result = search();
        assertEquals("Ella, Sri Lanka", result.location()); assertEquals("Test Cafe", result.places().getFirst().name());
        assertEquals(2, paths.size()); assertFalse(paths.get(0).contains("countrycode:"));
        String query = java.net.URLDecoder.decode(paths.get(1), StandardCharsets.UTF_8);
        assertTrue(query.contains("categories=catering.restaurant,catering.cafe"));
        assertTrue(query.contains("circle:81.0466,6.8667,10000")); assertTrue(query.contains("offset=0"));
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("apiKey"));
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("must-not-leak"));
    }
    @Test void paginationReusesCoordinatesAndPassesPlaceNameFilter() {
        service.search("Ella", "HOTEL", "Garden", 5000, 1, 6.8667, 81.0466);
        assertEquals(1, paths.size()); assertTrue(paths.getFirst().contains("/v2/places?"));
        assertTrue(paths.getFirst().contains("offset=12")); assertTrue(paths.getFirst().contains("name=Garden"));
    }
    @Test void detailsExposeOnlySelectedFields() {
        var place = service.detail("place_1");
        assertEquals("0123456789", place.phone()); assertEquals("Mo-Fr 09:00-17:00", place.openingHours());
        assertTrue(paths.getFirst().contains("features=details"));
    }
    @Test void acceptsCanonicalIdsInThePrimaryDetailsFeatureAndPreservesRequestIdentity() {
        places.set(places.get().replace("place_1", "canonical_place_2")
            .replace("\"properties\":{", "\"properties\":{\"feature_type\":\"details\","));
        var place = service.detail("place_1");
        assertEquals("place_1", place.id());
        assertEquals("Test Cafe", place.name());
        service.detail("place_1");
        assertEquals(1, paths.size());
    }
    @Test void doesNotAcceptANearbyPlaceAsTheRequestedDetails() {
        places.set(places.get().replace("\"properties\":{", "\"properties\":{\"feature_type\":\"radius_500.restaurant\","));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.detail("place_1")).getStatusCode().value());
    }
    @Test void supportsForeignDestinationsAndRetainsForeignPlaceResults() {
        geocode.set("{\"results\":[{\"lat\":51.5074,\"lon\":-0.1278,\"formatted\":\"London, UK\"}]}");
        places.set(places.get().replace("6.87", "51.5074").replace("81.04", "-0.1278").replace("\"lk\"", "\"gb\""));
        var result=service.search("London, UK","RESTAURANT","",10000,0,null,null);
        assertEquals(1,result.places().size());assertEquals(51.5074,result.latitude());assertEquals(-0.1278,result.longitude());
        assertFalse(paths.getFirst().contains("countrycode:"));
    }
    @Test void rejectsInvalidInputBeforeNetwork() {
        assertThrows(ResponseStatusException.class, () -> service.search("", "", "", 10000, 0, null, null));
        assertThrows(ResponseStatusException.class, () -> service.search("Ella", "INVALID", "", 10000, 0, null, null));
        assertThrows(ResponseStatusException.class, () -> service.search("Ella", "", "", 10000, 0, 6.8, null));
        assertThrows(ResponseStatusException.class, () -> service.search("Ella", "", "", 0, 0, null, null));
        assertThrows(ResponseStatusException.class, () -> service.search("Ella", "", "", 10000, -1, null, null));
        assertThrows(ResponseStatusException.class, () -> service.search("Ella", "", "", 10000, 0, Double.NaN, 81.0));
        assertThrows(ResponseStatusException.class, () -> service.detail("../secret"));
        assertTrue(paths.isEmpty());
    }
    @Test void missingKeyFailsClearlyWithoutNetwork() {
        var missing = new GeoapifyService(RestClient.builder(), "", baseUrl());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> missing.search("Ella", "", "", 10000, 0, null, null)).getStatusCode().value());
        assertTrue(paths.isEmpty());
    }
    @Test void emptyGeocodingDoesNotSearchUnrelatedPlaces() {
        geocode.set("{\"results\":[]}");
        assertEquals(404, assertThrows(ResponseStatusException.class, this::search).getStatusCode().value());
        assertEquals(1, paths.size());
    }
    @Test void providerErrorsAreSanitized() {
        status.set(403);
        var exception = assertThrows(ResponseStatusException.class, this::search);
        assertEquals(502, exception.getStatusCode().value()); assertFalse(exception.getReason().contains("test-private-key"));
        status.set(429); assertEquals(429, assertThrows(ResponseStatusException.class, this::search).getStatusCode().value());
    }
    @Test void emptyOrMalformedDetailsDoNotFabricateAPlace() {
        places.set("{\"features\":[]}");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.detail("place_1")).getStatusCode().value());
        places.set("{}");
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.detail("place_1")).getStatusCode().value());
    }
}
