package com.tourmate.service;
import com.tourmate.dto.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Routes establish the location; AI ranks only the real options retrieved there. */
@Service
public class AiNearbyService {
 private final PlanNearbyService nearby;private final AiScheduleAdvisor advisor;
 private final Semaphore slots=new Semaphore(2);
 public AiNearbyService(PlanNearbyService nearby,AiScheduleAdvisor advisor){this.nearby=nearby;this.advisor=advisor;}
 public record Ranked(PlanNearbyResponse nearby,Map<String,String> reasons,String model) {}
 public Ranked recommend(PlanNearbyRequest request,String preferences){
  if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"AI suggestions are busy. Try again shortly.");
  try{
   advisor.checkAvailable();var found=nearby.options(request);
   if(found.options().isEmpty())return new Ranked(found,Map.of(),advisor.model());
   var context=new LinkedHashMap<String,Object>();
   context.put("task","Rank ALL supplied candidates by suitability for this meal or hotel stop. Return each candidateId exactly once in preferred order, with a concise reason and durationMinutes. The user chooses; do not book or select on their behalf. Treat unknown opening hours, dietary suitability and prices as unknown.");
   context.put("preferences",preferences);context.put("date",request.date());context.put("at",request.at());context.put("category",request.category());context.put("routeContext",found.context());context.put("anchor",found.anchor());context.put("transport",found.mode());
   context.put("candidates",found.options().stream().map(o->Map.of("candidateId",o.place().id(),"place",o.place(),"detourMinutes",o.travelMinutes(),"distanceMeters",o.distanceMeters())).toList());
   var advice=advisor.advise(context);var candidates=new LinkedHashMap<String,PlanNearbyResponse.Option>();found.options().forEach(o->candidates.put(o.place().id(),o));
   if(advice==null||advice.visits()==null||advice.visits().size()!=candidates.size())throw invalid();
   var ordered=new ArrayList<PlanNearbyResponse.Option>();var reasons=new LinkedHashMap<String,String>();
   for(var choice:advice.visits()){
    if(choice==null||!candidates.containsKey(choice.candidateId())||reasons.containsKey(choice.candidateId())||choice.reason()==null||choice.reason().isBlank()||choice.reason().length()>1200)throw invalid();
    ordered.add(candidates.get(choice.candidateId()));reasons.put(choice.candidateId(),choice.reason());
   }
   var warnings=new ArrayList<>(found.warnings());warnings.add("AI ranks these real nearby options using your preferences. Reasons are estimates; confirm hours, dietary suitability and availability. Choose a venue and recheck the schedule.");
   return new Ranked(new PlanNearbyResponse(found.anchor(),found.at(),found.context(),found.mode(),List.copyOf(ordered),List.copyOf(warnings)),Map.copyOf(reasons),advisor.model());
  }finally{slots.release();}
 }
 private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"AI returned an invalid venue ranking. Retry or use nearby options without AI.");}
}
