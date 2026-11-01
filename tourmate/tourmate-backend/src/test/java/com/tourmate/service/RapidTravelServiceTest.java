package com.tourmate.service;
import com.tourmate.dto.TravelSearchResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class RapidTravelServiceTest {
    private HttpServer server;
    private RapidTravelService service;
    private final List<String> paths = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode posted;
    private String expectedListPath = "/attractions/v2/list";
    private String listResponse = """
        {"data":{"AppPresentation_queryAppListV2":[{"sections":[
          {"listSingleCardContent":{"saveId":{"id":"3477576"},
            "cardTitle":{"string":"1. Thirukoneswaram Kovil"},
            "primaryInfo":{"text":"Religious Sites"},
            "cardPhoto":{"sizes":{"urlTemplate":"https://example.org/photo?w={width}&h={height}"}},
            "bubbleRating":{"rating":4.4,"numberReviews":{"string":"(1,234)"}},
            "cardLink":{"route":{"url":"/AppDetail?contentType=attraction&contentId=3477576"}}}},
          {"__typename":"AppPresentation_Banner"}
        ]}]}}
        """;
    private String locationResponse = """
        {"data":{"Typeahead_autocomplete":{"results":[
          {"detailsV2":{"locationId":999,"isGeo":false,"geocode":{"latitude":8.577,"longitude":81.225}}},
          {"detailsV2":{"locationId":424963,"isGeo":true,"geocode":{"latitude":8.577364,"longitude":81.2254}}}
        ]}}}
        """;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            String response;
            if (exchange.getRequestURI().getPath().equals("/locations/v2/auto-complete")) {
                assertEquals("GET", exchange.getRequestMethod());
                response = locationResponse;
            } else {
                assertEquals(expectedListPath, exchange.getRequestURI().getPath());
                assertEquals("POST", exchange.getRequestMethod());
                assertTrue(exchange.getRequestHeaders().getFirst("Content-Type").contains("application/json"));
                assertEquals("test-key", exchange.getRequestHeaders().getFirst("x-rapidapi-key"));
                posted = mapper.readTree(exchange.getRequestBody());
                response = listResponse;
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) {stream.write(bytes);}
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        service = new RapidTravelService(null, RestClient.builder(), "test-key", "travel-advisor.p.rapidapi.com", "booking-com18.p.rapidapi.com", base, base);
    }
    @AfterEach void stop() {server.stop(0);}
    private TravelSearchResult search(String type, int page) {
        return service.search("Trincomalee", type, 10000, page, 8.577364, 81.2254, null, null, 1, 2);
    }
    @Test void usesV2DestinationAndPostBodyAndMapsRealCardShape() {
        var result = search("ATTRACTION", 0);
        assertEquals(List.of("/locations/v2/auto-complete", "/attractions/v2/list"), paths);
        assertEquals(424963, posted.path("geoId").asInt());
        assertTrue(posted.path("boundingBox").path("northEastCorner").path("latitude").asDouble() > 8.577364);
        assertTrue(posted.path("boundingBox").path("southWestCorner").path("longitude").asDouble() < 81.2254);
        assertFalse(posted.has("startDate"));
        assertTrue(posted.path("filters").isEmpty());
        assertEquals(1, result.places().size());
        var place = result.places().getFirst();
        assertEquals("3477576", place.id());
        assertEquals("Thirukoneswaram Kovil", place.name());
        assertEquals(1234, place.reviewCount());
        assertEquals(4.4, place.rating());
        assertEquals("https://example.org/photo?w=800&h=600", place.photoUrl());
        assertFalse(result.hasMore());
        assertEquals(place, service.detail("ATTRACTION", place.id(), place.resultToken()));
        assertEquals(2, paths.size());
    }
    @Test void restaurantsUseCurrentEndpointAndRestaurantSortAndRetainSearchDetails() {
        expectedListPath = "/restaurants/v2/list";
        listResponse = listResponse.replace("3477576", "14083512").replace("Thirukoneswaram Kovil", "Ubay Seafood Restaurant")
            .replace("Religious Sites", "Seafood · Asian").replace("contentType=attraction", "contentType=restaurant");
        var result = search("RESTAURANT", 0);
        assertEquals(List.of("/locations/v2/auto-complete", "/restaurants/v2/list"), paths);
        assertEquals("POPULARITY", posted.path("sort").asText());
        assertEquals("desc", posted.path("sortOrder").asText());
        assertEquals(424963, posted.path("geoId").asInt());
        var place = result.places().getFirst();
        assertEquals("RESTAURANT", place.type());
        assertEquals("Ubay Seafood Restaurant", place.name());
        assertEquals("Seafood · Asian", place.description());
        assertEquals(place, service.detail("RESTAURANT", place.id(), place.resultToken()));
        assertEquals(2, paths.size());
        assertThrows(ResponseStatusException.class, () -> service.detail("RESTAURANT", place.id(), "expired"));
        assertThrows(ResponseStatusException.class, () -> search("RESTAURANT", 1));
        assertEquals(2, paths.size());
    }
    @Test void activitiesAlsoUseV2AndExpiredDetailsDoNotCallDeprecatedEndpoint() {
        assertEquals("ACTIVITY", search("ACTIVITY", 0).places().getFirst().type());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.detail("ACTIVITY", "3477576", "expired")).getStatusCode().value());
        assertEquals(2, paths.size());
    }
    @Test void mapLookupUsesOnlyCoordinatesForTheExactProviderId() {
        var place = search("ATTRACTION", 0).places().getFirst();
        locationResponse = """
            {"data":{"Typeahead_autocomplete":{"results":[
              {"detailsV2":{"locationId":999,"geocode":{"latitude":8.0,"longitude":81.0}}},
              {"detailsV2":{"locationId":3477576,"geocode":{"latitude":8.574719,"longitude":81.23472}}}
            ]}}}
            """;
        var location = service.mapLocation("ATTRACTION", place.id(), place.resultToken());
        assertEquals(8.574719, location.latitude());
        assertEquals(81.23472, location.longitude());
        locationResponse = locationResponse.replace("3477576", "12345");
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.mapLocation("ATTRACTION", place.id(), place.resultToken())).getStatusCode().value());
        locationResponse = locationResponse.replace("12345", "3477576").replace("8.574719", "95.0");
        assertThrows(ResponseStatusException.class, () -> service.mapLocation("ATTRACTION", place.id(), place.resultToken()));
    }
    @Test void mapLookupRejectsExpiredOrMismatchedSearchResultsBeforeNetwork() {
        var place = search("ATTRACTION", 0).places().getFirst();
        assertThrows(ResponseStatusException.class, () -> service.mapLocation("ATTRACTION", place.id(), "expired"));
        assertThrows(ResponseStatusException.class, () -> service.mapLocation("RESTAURANT", place.id(), place.resultToken()));
        assertThrows(ResponseStatusException.class, () -> service.mapLocation("ATTRACTION", "12345", place.resultToken()));
        assertEquals(2, paths.size());
    }
    @Test void kandyUsesTravelAdvisorDestinationCoordinatesWithoutAnUnrelatedGeocoder() {
        locationResponse = """
            {"data":{"Typeahead_autocomplete":{"results":[
              {"detailsV2":{"locationId":999,"isGeo":false,"geocode":{"latitude":6.9061459,"longitude":79.8514444}}},
              {"detailsV2":{"locationId":304138,"isGeo":true,"names":{"name":"Kandy"},"geocode":{"latitude":7.293553,"longitude":80.63852}}}
            ]}}}
            """;
        // No Geoapify service is injected: an ambiguous geocoder result must not affect this search.
        var result = service.search("kandy", "ATTRACTION", 10000, 0, null, null, null, null, 1, 2);
        assertEquals(304138, posted.path("geoId").asInt());
        assertEquals(7.293553, result.latitude()); assertEquals(80.63852, result.longitude()); assertEquals("Kandy", result.location());
        assertEquals(2, paths.size());
        assertTrue(posted.path("boundingBox").path("northEastCorner").path("latitude").asDouble() > 7.293553);
    }
    @Test void restaurantsResolveTheirOwnDestinationAndInvalidCoordinatesNeverReachProvider() {
        expectedListPath = "/restaurants/v2/list";
        var result = service.search("Trincomalee", "RESTAURANT", 10000, 0, null, null, null, null, 1, 2);
        assertEquals(8.577364, result.latitude()); assertEquals(424963, posted.path("geoId").asInt());
        assertThrows(ResponseStatusException.class, () -> service.search("Kandy", "ATTRACTION", 10000, 0, Double.NaN, 80.6, null, null, 1, 2));
        assertEquals(2, paths.size());
    }
    @Test void resolvesProviderDestinationsAcrossCountriesAndHemispheres() {
        double[][] coordinates={{51.5074,-0.1278},{35.6762,139.6503},{-33.8688,151.2093},{40.7128,-74.0060}};
        String[] names={"London, UK","Tokyo, Japan","Sydney, Australia","New York, USA"};
        for(int i=0;i<coordinates.length;i++) {
            locationResponse="{\"data\":{\"Typeahead_autocomplete\":{\"results\":[{\"detailsV2\":{\"locationId\":"+(1000+i)+",\"isGeo\":true,\"geocode\":{\"latitude\":"+coordinates[i][0]+",\"longitude\":"+coordinates[i][1]+"}}}]}}}";
            var result=service.search(names[i],"ATTRACTION",10000,0,null,null,null,null,1,2);
            assertEquals(1000+i,posted.path("geoId").asInt());
            assertEquals(coordinates[i][0],result.latitude());assertEquals(coordinates[i][1],result.longitude());
        }
    }
    @Test void rejectsUnverifiedPaginationBeforeProviderCalls() {
        assertThrows(ResponseStatusException.class, () -> search("ATTRACTION", 1));
        assertTrue(paths.isEmpty());
    }
    @Test void rejectsDestinationFarFromRequestedCoordinates() {
        locationResponse = locationResponse.replace("8.577364", "12.0");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> search("ATTRACTION", 0)).getStatusCode().value());
        assertEquals(1, paths.size());
    }
    @Test void rejectsUnsupportedResponsesRatherThanShowingAnEmptySearch() {
        listResponse = "{\"data\":{}}";
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> search("ATTRACTION", 0)).getStatusCode().value());
    }
}
