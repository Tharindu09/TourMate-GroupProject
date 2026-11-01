package com.tourmate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tourmate.dto.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeoRoutingService {
 private final RestClient client;private final String key;
 private record Cached(RouteEstimate route,long expires) {}
 private final Map<String,Cached> cache=new ConcurrentHashMap<>();
 private long window=System.nanoTime();private int calls;
 public GeoRoutingService(RestClient.Builder builder,@Value("${app.geoapify.api-key:}")String key,@Value("${app.geoapify.base-url:https://api.geoapify.com}")String base){
  this.key=key.strip();var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(20));client=builder.clone().baseUrl(base).requestFactory(factory).build();
 }
 public String providerMode(String mode){return switch(mode){case "CAR"->"drive";case "WALKING"->"walk";case "CYCLING"->"bicycle";case "PUBLIC_TRANSPORT"->"transit";default->throw error(HttpStatus.BAD_REQUEST,"Choose driving, walking, cycling or public transport for route estimates.");};}
 private synchronized void permit(){if(key.isBlank())throw error(HttpStatus.SERVICE_UNAVAILABLE,"Routing is not configured. Set GEOAPIFY_API_KEY and enable Routing API access.");long now=System.nanoTime();if(now-window>Duration.ofMinutes(1).toNanos()){window=now;calls=0;}if(++calls>120)throw error(HttpStatus.TOO_MANY_REQUESTS,"Routing request limit reached. Wait a minute before rebuilding the schedule.");}
 public RouteEstimate route(RoutePoint from,RoutePoint to,String mode){
  String profile=providerMode(mode);validate(from);validate(to);
  if(Math.abs(from.latitude()-to.latitude())<0.000001&&Math.abs(from.longitude()-to.longitude())<0.000001)return new RouteEstimate(0,0,List.of(from,to),List.of(0.0,0.0));
  String id=profile+":"+from.latitude()+","+from.longitude()+":"+to.latitude()+","+to.longitude();Cached saved=cache.get(id);if(saved!=null&&saved.expires()>System.currentTimeMillis())return saved.route();
  permit();
  try {
   JsonNode data=client.get().uri(b->{b.path("/v1/routing").queryParam("waypoints","{points}").queryParam("mode",profile).queryParam("units","metric");if("CAR".equals(mode))b.queryParam("traffic","approximated");return b.queryParam("apiKey",key).build(from.latitude()+","+from.longitude()+"|"+to.latitude()+","+to.longitude());}).retrieve().body(JsonNode.class);
   JsonNode feature=data==null?null:data.path("features").path(0);if(feature==null||!feature.path("properties").path("time").isNumber())throw noRoute(from,to,mode);
   double seconds=feature.path("properties").path("time").asDouble(),distance=feature.path("properties").path("distance").asDouble(-1);
   if(!Double.isFinite(seconds)||seconds<0||!Double.isFinite(distance)||distance<0)throw error(HttpStatus.BAD_GATEWAY,"Routing provider returned invalid travel estimates.");
   List<RoutePoint> points=new ArrayList<>();JsonNode geometry=feature.path("geometry");
   if("LineString".equals(geometry.path("type").asText()))readLine(geometry.path("coordinates"),points);else for(JsonNode line:geometry.path("coordinates"))readLine(line,points);
   if(points.size()<2)throw error(HttpStatus.BAD_GATEWAY,"Routing provider did not return a mapped route.");
   double[] times=new double[points.size()];Arrays.fill(times,Double.NaN);times[0]=0;double elapsed=0;
   for(JsonNode step:feature.path("properties").path("legs").path(0).path("steps")){
    int a=step.path("from_index").asInt(-1),z=step.path("to_index").asInt(-1);double duration=step.path("time").asDouble(-1);
    if(a<0||z<=a||z>=points.size()||duration<0)continue;
    double length=0;for(int i=a+1;i<=z;i++)length+=distance(points.get(i-1),points.get(i));double covered=0;
    times[a]=elapsed;for(int i=a+1;i<=z;i++){covered+=distance(points.get(i-1),points.get(i));times[i]=elapsed+duration*(length==0?(double)(i-a)/(z-a):covered/length);}elapsed+=duration;
   }
   List<RoutePoint> sampled=new ArrayList<>();List<Double> sampledTimes=new ArrayList<>();int stride=Math.max(1,(int)Math.ceil(points.size()/400.0));
   double previous=0;for(int i=0;i<points.size();i+=stride){sampled.add(points.get(i));double time=Double.isFinite(times[i])?times[i]:seconds*i/(points.size()-1);time=Math.min(seconds,Math.max(previous,time));sampledTimes.add(time);previous=time;}
   if(!sampled.getLast().equals(points.getLast())){sampled.add(points.getLast());sampledTimes.add(seconds);}
   var result=new RouteEstimate((int)Math.ceil(seconds/60),distance,List.copyOf(sampled),List.copyOf(sampledTimes));
   if(cache.size()>=2048)cache.clear();cache.put(id,new Cached(result,System.currentTimeMillis()+Duration.ofMinutes(15).toMillis()));return result;
  }catch(RestClientResponseException ex){int status=ex.getStatusCode().value();if(status==400||status==404)throw noRoute(from,to,mode);if(status==401||status==403)throw error(HttpStatus.BAD_GATEWAY,"Routing access was rejected. Check the Geoapify key and Routing API permissions.");if(status==429)throw error(HttpStatus.TOO_MANY_REQUESTS,"Routing provider quota reached.");throw error(HttpStatus.BAD_GATEWAY,"The routing provider is unavailable.");}
  catch(RestClientException ex){throw error(HttpStatus.BAD_GATEWAY,"Could not reach the routing provider. No estimated travel time was substituted.");}
 }
 public RoutePoint position(RouteEstimate route,double elapsedMinutes){
  double seconds=Math.max(0,elapsedMinutes*60);var points=route.geometry();var times=route.elapsedSeconds();
  for(int i=1;i<points.size();i++)if(seconds<=times.get(i)){double span=times.get(i)-times.get(i-1),fraction=span<=0?0:Math.max(0,Math.min(1,(seconds-times.get(i-1))/span));var a=points.get(i-1);var b=points.get(i);return new RoutePoint(a.latitude()+(b.latitude()-a.latitude())*fraction,a.longitude()+(b.longitude()-a.longitude())*fraction,"Estimated position along route");}
  return points.getLast();
 }
 private void readLine(JsonNode line,List<RoutePoint> output){for(JsonNode coordinate:line)if(coordinate.isArray()&&coordinate.size()>=2&&coordinate.path(0).isNumber()&&coordinate.path(1).isNumber()){var p=new RoutePoint(coordinate.get(1).asDouble(),coordinate.get(0).asDouble(),"");validate(p);output.add(p);}}
 private void validate(RoutePoint p){if(!Double.isFinite(p.latitude())||!Double.isFinite(p.longitude())||Math.abs(p.latitude())>90||Math.abs(p.longitude())>180)throw error(HttpStatus.BAD_REQUEST,"Valid map coordinates are required for routing.");}
 private double distance(RoutePoint a,RoutePoint b){double lat=Math.toRadians(b.latitude()-a.latitude()),lon=Math.toRadians(b.longitude()-a.longitude());double x=Math.pow(Math.sin(lat/2),2)+Math.cos(Math.toRadians(a.latitude()))*Math.cos(Math.toRadians(b.latitude()))*Math.pow(Math.sin(lon/2),2);return 6371000*2*Math.asin(Math.min(1,Math.sqrt(x)));}
 private ResponseStatusException noRoute(RoutePoint a,RoutePoint b,String mode){return error(HttpStatus.UNPROCESSABLE_ENTITY,"No "+mode.toLowerCase().replace('_',' ')+" route is available from "+a.label()+" to "+b.label()+". Change the mode or selected stops.");}
 private ResponseStatusException error(HttpStatus status,String text){return new ResponseStatusException(status,text);}
}
