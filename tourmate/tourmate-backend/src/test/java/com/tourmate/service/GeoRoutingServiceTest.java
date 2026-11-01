package com.tourmate.service;
import com.tourmate.dto.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class GeoRoutingServiceTest {
 private HttpServer server;private GeoRoutingService routing;private final List<String> queries=new ArrayList<>();private int status=200;private boolean empty=false;
 private final RoutePoint a=new RoutePoint(7,80,"Start"),b=new RoutePoint(7.02,80.02,"End");
 @BeforeEach void setup()throws Exception{
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/v1/routing",e->{String query=URLDecoder.decode(e.getRequestURI().getRawQuery(),StandardCharsets.UTF_8);queries.add(query);int seconds=query.contains("mode=walk")?1200:240;String body=empty?"{\"features\":[]}":"{\"features\":[{\"properties\":{\"time\":"+seconds+",\"distance\":3000,\"legs\":[{\"steps\":[{\"from_index\":0,\"to_index\":1,\"time\":"+(seconds/4)+"},{\"from_index\":1,\"to_index\":2,\"time\":"+(seconds*3/4)+"}]}]},\"geometry\":{\"type\":\"MultiLineString\",\"coordinates\":[[[80,7],[80.01,7.01],[80.02,7.02]]]}}]}";if(status!=200)body="test-secret must never be shown";byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(status,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}});server.start();routing=new GeoRoutingService(RestClient.builder(),"test-secret","http://127.0.0.1:"+server.getAddress().getPort());
 }
 @AfterEach void stop(){server.stop(0);}
 @Test void usesActualProviderDurationsAndSeparateProfilesAndCachesDirectedLegs(){assertEquals(4,routing.route(a,b,"CAR").minutes());assertEquals(20,routing.route(a,b,"WALKING").minutes());assertEquals(4,routing.route(a,b,"CAR").minutes());assertEquals(2,queries.size());assertTrue(queries.getFirst().contains("waypoints=7.0,80.0|7.02,80.02"));assertTrue(queries.getFirst().contains("traffic=approximated"));routing.route(b,a,"CAR");assertEquals(3,queries.size());}
 @Test void predictsPositionUsingRouteStepTimes(){var route=routing.route(a,b,"CAR");var halfway=routing.position(route,2);assertEquals(7.0133333,halfway.latitude(),0.00001);assertEquals(80.0133333,halfway.longitude(),0.00001);}
 @Test void transitAndCyclingUseTheirOwnProviderModes(){routing.route(a,b,"PUBLIC_TRANSPORT");routing.route(a,b,"CYCLING");assertTrue(queries.get(0).contains("mode=transit"));assertTrue(queries.get(1).contains("mode=bicycle"));}
 @Test void noRouteDoesNotProduceAFakeDuration(){empty=true;assertEquals(422,assertThrows(ResponseStatusException.class,()->routing.route(a,b,"PUBLIC_TRANSPORT")).getStatusCode().value());assertEquals(1,queries.size());}
 @Test void rejectsUnsupportedModeAndInvalidCoordinates(){assertThrows(ResponseStatusException.class,()->routing.route(a,b,"OTHER"));assertThrows(ResponseStatusException.class,()->routing.route(new RoutePoint(91,80,"Bad"),b,"CAR"));assertTrue(queries.isEmpty());}
 @Test void sanitizesProviderAuthenticationErrors(){status=403;var ex=assertThrows(ResponseStatusException.class,()->routing.route(a,b,"CAR"));assertEquals(502,ex.getStatusCode().value());assertFalse(ex.getReason().contains("test-secret"));}
}
