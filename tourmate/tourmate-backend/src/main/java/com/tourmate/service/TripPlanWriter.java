package com.tourmate.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourmate.dto.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TripPlanWriter {
 private final TripService trips;private final ObjectMapper json;
 public TripPlanWriter(TripService trips,ObjectMapper json){this.trips=trips;this.json=json;}
 private record Entry(TripItemRequest item,String meal) {}
 @Transactional public TripResponse save(Long userId,ManualPlanRequest input,ManualPlanPreview preview){return write(userId,input,preview,null);}
 @Transactional public TripResponse saveAi(Long userId,ManualPlanRequest input,ManualPlanPreview preview,AiPlanResponse ai){return write(userId,input,preview,ai);}
 private TripResponse write(Long userId,ManualPlanRequest input,ManualPlanPreview preview,AiPlanResponse ai){
  var created=trips.create(userId,input.trip());
  for(var day:preview.days()){
   Long id=created.days().stream().filter(d->d.date().equals(day.date())).findFirst().orElseThrow().id();List<Entry> entries=new ArrayList<>();
   for(var visit:day.visits()){var s=visit.stop();entries.add(new Entry(new TripItemRequest(id,s.title(),s.category(),s.placeSource(),s.placeId(),s.address(),s.latitude(),s.longitude(),visit.startTime(),visit.endTime(),s.estimatedCost(),s.notes()),s.mealType()));}
   for(var leg:day.travelLegs()){
    String title="Travel: "+leg.from().label()+" → "+leg.to().label();if(title.length()>300)title=title.substring(0,297)+"…";
    String notes=leg.mode()+" · estimated "+leg.route().minutes()+" minutes · "+Math.round(leg.route().distanceMeters())+" m. Geoapify/OpenStreetMap routing estimate; check traffic and transit schedules.";
    entries.add(new Entry(new TripItemRequest(id,title,"TRANSPORT","CUSTOM",null,leg.to().label(),leg.to().latitude(),leg.to().longitude(),leg.departureTime(),leg.arrivalTime(),BigDecimal.ZERO,notes),"NONE"));
   }
   entries.sort(Comparator.comparing((Entry e)->e.item().startTime()).thenComparing(e->e.item().endTime()));
   for(var entry:entries)trips.addPlannedItem(userId,created.id(),entry.item(),entry.meal());
  }
  try{ObjectNode preferences=json.valueToTree(input);if(ai!=null){preferences.set("ai",json.valueToTree(java.util.Map.of("model",ai.model(),"explanations",ai.explanations(),"warnings",ai.warnings())));trips.markAi(userId,created.id());}preferences.set("routePreview",json.valueToTree(preview));preferences.set("savedSchedule",json.valueToTree(trips.detail(userId,created.id()).days()));trips.recordPlanningPreferences(userId,created.id(),json.writeValueAsString(preferences));}
  catch(JsonProcessingException ex){throw new IllegalStateException("Could not save route planning preferences",ex);}
  return trips.detail(userId,created.id());
 }
}
