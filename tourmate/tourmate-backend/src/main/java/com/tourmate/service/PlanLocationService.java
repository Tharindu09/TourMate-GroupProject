package com.tourmate.service;
import com.tourmate.dto.*;
import com.tourmate.repository.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PlanLocationService {
 private record Cached(RoutePoint point,long expires) {}
 private final java.util.concurrent.ConcurrentHashMap<String,Cached> cache=new java.util.concurrent.ConcurrentHashMap<>();
 private final GeoapifyService geo;private final RapidTravelService travel;private final PlaceRepository places;
 public PlanLocationService(GeoapifyService geo,RapidTravelService travel,PlaceRepository places){this.geo=geo;this.travel=travel;this.places=places;}
 public RoutePoint origin(ManualPlanRequest input){
  if(input.startingLatitude()!=null&&input.startingLongitude()!=null)return new RoutePoint(input.startingLatitude(),input.startingLongitude(),input.startingLocation());
  if(input.startingLatitude()!=null||input.startingLongitude()!=null)throw bad("Enter both starting coordinates.");
  String cacheKey="origin:"+input.startingLocation().strip();var saved=cache.get(cacheKey);if(saved!=null&&saved.expires()>System.currentTimeMillis())return saved.point();
  var point=geo.locate(input.startingLocation());var result=new RoutePoint(point.latitude(),point.longitude(),point.name());remember(cacheKey,result);return result;
 }
 public ManualPlanRequest.Stop resolve(ManualPlanRequest.Stop stop){
  if(!"CUSTOM".equals(stop.placeSource())&&(stop.placeId()==null||stop.placeId().isBlank()))throw bad("The selected place needs a valid provider ID.");
  if((stop.latitude()==null)!=(stop.longitude()==null))throw bad("Enter both coordinates for "+stop.title());
  boolean verified=false;
  Double lat=stop.latitude(),lon=stop.longitude();String cacheKey=stop.placeSource()+":"+stop.placeId();var saved=cache.get(cacheKey);
  if(!"CUSTOM".equals(stop.placeSource())&&saved!=null&&saved.expires()>System.currentTimeMillis()){lat=saved.point().latitude();lon=saved.point().longitude();verified=true;}

  if("TOURMATE".equals(stop.placeSource())){try{var place=places.findById(Long.valueOf(stop.placeId())).orElseThrow(()->bad("Selected place no longer exists."));lat=place.latitude;lon=place.longitude;verified=true;}catch(NumberFormatException e){throw bad("Invalid selected place ID.");}}
  else if(lat==null||lon==null){
   if("RAPIDAPI".equals(stop.placeSource())){var point=travel.resolveCoordinates(stop.placeId(),stop.title());lat=point.latitude();lon=point.longitude();verified=true;}
   else if("GEOAPIFY".equals(stop.placeSource())){var point=geo.detail(stop.placeId());lat=point.latitude();lon=point.longitude();verified=true;}
   else throw bad("Add map coordinates for '"+stop.title()+"' or choose a real venue from suggestions. A name-only placeholder cannot be routed.");
  }
  if(lat==null||lon==null||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw bad("Invalid coordinates for "+stop.title());
  if(verified)remember(cacheKey,new RoutePoint(lat,lon,stop.title()));
  return new ManualPlanRequest.Stop(stop.key(),stop.title(),stop.category(),stop.placeSource(),stop.placeId(),stop.address(),lat,lon,stop.durationMinutes(),stop.preferredTime(),stop.estimatedCost(),stop.notes(),stop.mealType());
 }
 private void remember(String key,RoutePoint point){if(cache.size()>=512)cache.clear();cache.put(key,new Cached(point,System.currentTimeMillis()+900000));}
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,text);}
}
