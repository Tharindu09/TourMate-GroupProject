package com.tourmate.service;
import com.tourmate.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PlanNearbyService {
 private final ManualSchedulePlanner planner;private final GeoRoutingService routing;private final GeoapifyService places;
 public PlanNearbyService(ManualSchedulePlanner planner,GeoRoutingService routing,GeoapifyService places){this.planner=planner;this.routing=routing;this.places=places;}
 public PlanNearbyResponse options(PlanNearbyRequest request){
  return options(request,planner.suggestionContext(request.plan()));
 }
 public PlanMealOptionsResponse meals(PlanMealOptionsRequest request){
  var requestedDay=request.plan().days().stream().filter(d->d.date().equals(request.date())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a date inside the trip."));
  var pending=requestedDay.stops().stream().filter(s->!"NONE".equals(s.mealType())&&"CUSTOM".equals(s.placeSource())&&(s.latitude()==null||s.longitude()==null)).toList();
  if(pending.size()>3)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Automatic meal suggestions support breakfast, lunch and dinner once each per day.");
  if(pending.isEmpty())return new PlanMealOptionsResponse(List.of());
  var preview=planner.suggestionContext(request.plan());List<PlanMealOptionsResponse.Meal> result=new ArrayList<>();
  for(var meal:pending){
   if(meal.preferredTime()==null){result.add(new PlanMealOptionsResponse.Meal(meal.key(),meal.mealType(),null,null,"Set a meal time to find venues."));continue;}
   try{var nearby=options(new PlanNearbyRequest(request.plan(),request.date(),meal.preferredTime(),"RESTAURANT"),preview);result.add(new PlanMealOptionsResponse.Meal(meal.key(),meal.mealType(),meal.preferredTime(),nearby,null));}
   catch(ResponseStatusException ex){result.add(new PlanMealOptionsResponse.Meal(meal.key(),meal.mealType(),meal.preferredTime(),null,ex.getReason()));}
  }
  return new PlanMealOptionsResponse(List.copyOf(result));
 }
 private PlanNearbyResponse options(PlanNearbyRequest request,ManualPlanPreview preview){
  var day=preview.days().stream().filter(d->d.date().equals(request.date())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a date inside the trip."));
  var preferences=request.plan().days().stream().filter(d->d.date().equals(request.date())).findFirst().orElseThrow();
  if(request.at().isBefore(preferences.startTime())||request.at().isAfter(preferences.endTime()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a recommendation time inside this day's travel hours.");
  RoutePoint anchor=day.origin();String context="At the day's starting location";
  for(var leg:day.travelLegs()){
   if(!request.at().isBefore(leg.departureTime())&&!request.at().isAfter(leg.arrivalTime())){anchor=routing.position(leg.route(),Duration.between(leg.departureTime(),request.at()).toSeconds()/60.0);context="Estimated position while traveling from "+leg.from().label()+" to "+leg.to().label();break;}
   if(!request.at().isBefore(leg.arrivalTime())){anchor=leg.to();context="Near "+anchor.label();}
  }
  for(var visit:day.visits())if(!request.at().isBefore(visit.startTime())&&!request.at().isAfter(visit.endTime())){anchor=new RoutePoint(visit.stop().latitude(),visit.stop().longitude(),visit.stop().title());context="During the planned visit to "+visit.stop().title();break;}
  int radius="WALKING".equals(request.plan().transportMode())?2000:5000;
  var results=places.search("Route position",request.category(),"",radius,0,anchor.latitude(),anchor.longitude());List<PlanNearbyResponse.Option> options=new ArrayList<>();
  for(var p:results.places().stream().limit(6).toList())try{var estimate=routing.route(anchor,new RoutePoint(p.latitude(),p.longitude(),p.name()),request.plan().transportMode());options.add(new PlanNearbyResponse.Option(p,estimate.minutes(),estimate.distanceMeters()));}catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=422)throw ex;}
  options.sort(Comparator.comparingInt(PlanNearbyResponse.Option::travelMinutes));
  List<String> warnings=new ArrayList<>(preview.warnings());for(var skipped:preview.unscheduled())warnings.add(skipped.title()+": "+skipped.reason());warnings.addAll(List.of("Locations along a travel leg are estimated from route steps, not GPS. Rebuild after selecting a venue to check the full detour and arrival time.","Confirm opening hours, dietary suitability, hotel availability and hotel dining access directly with the venue."));
  return new PlanNearbyResponse(anchor,request.at(),context,request.plan().transportMode(),options,List.copyOf(warnings));
 }
}
