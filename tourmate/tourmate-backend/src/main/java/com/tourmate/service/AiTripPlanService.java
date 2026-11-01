package com.tourmate.service;
import com.tourmate.dto.*;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiTripPlanService {
 private final AiScheduleAdvisor advisor;private final GeoapifyService places;private final ManualSchedulePlanner planner;
 private final PlanNearbyService nearby;private final TripPlanWriter writer;private final Validator validator;
 private record Draft(Long owner,Instant expires,AiPlanResponse response) {}
 private final Map<String,Draft> drafts=new ConcurrentHashMap<>();
 private final Set<Long> activeUsers=ConcurrentHashMap.newKeySet();
 private final java.util.concurrent.Semaphore slots=new java.util.concurrent.Semaphore(2);
 public AiTripPlanService(AiScheduleAdvisor advisor,GeoapifyService places,ManualSchedulePlanner planner,PlanNearbyService nearby,TripPlanWriter writer,Validator validator){this.advisor=advisor;this.places=places;this.planner=planner;this.nearby=nearby;this.writer=writer;this.validator=validator;}
 public AiPlanResponse generate(Long owner,AiPlanRequest request){
  if(!activeUsers.add(owner))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"An AI draft is already being generated for your account.");
  if(!slots.tryAcquire()){activeUsers.remove(owner);throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"AI planning is busy. Try again shortly.");}
  try{return generateDraft(owner,request);}finally{slots.release();activeUsers.remove(owner);}
 }
 private AiPlanResponse generateDraft(Long owner,AiPlanRequest request){
  advisor.checkAvailable();
  var input=request.plan();
  if(input.days().size()>7||input.days().stream().mapToInt(d->d.stops().size()).sum()>40)throw bad("AI drafts support up to 7 days and 40 selected stops. Plan longer journeys in sections.");
  if(input.days().stream().anyMatch(d->!d.startTime().isBefore(d.endTime())))throw bad("Daily finish must be after start.");
  List<String> explanations=new ArrayList<>(),warnings=new ArrayList<>(List.of("AI visit durations are estimates based on available place context. Confirm opening hours, tickets, accessibility and dietary suitability with each venue.","Unpriced venues use zero as an unknown cost, not free admission. This draft does not establish that the trip fits your budget.","Meal venues are selected near predicted route positions; the final route includes their detours. Review conflicts and adjust times before saving."));
  if(input.trip().endDate().isBefore(input.trip().startDate())||java.time.temporal.ChronoUnit.DAYS.between(input.trip().startDate(),input.trip().endDate())+1!=input.days().size()||input.days().stream().map(ManualPlanRequest.Day::date).distinct().count()!=input.days().size()||input.days().stream().anyMatch(d->d.date().isBefore(input.trip().startDate())||d.date().isAfter(input.trip().endDate())))throw bad("Supply each trip date exactly once, within a journey of 1–7 days.");
  List<ManualPlanRequest.Day> days=new ArrayList<>();Set<String> usedVenues=new HashSet<>();
  for(var day:input.days()){
   Map<String,ManualPlanRequest.Stop> candidates=new LinkedHashMap<>();Map<String,Object> evidence=new LinkedHashMap<>();Set<String> required=new HashSet<>();
   for(var stop:day.stops())if("NONE".equals(stop.mealType())||stop.latitude()!=null){candidates.put(stop.key(),stop);required.add(stop.key());evidence.put(stop.key(),Map.of("candidateId",stop.key(),"place",stop,"required",true,"lockedDuration",stop.preferredTime()!=null));}
   if(candidates.values().stream().noneMatch(s->"NONE".equals(s.mealType()))){
    var found=places.search(day.area(),"ATTRACTION","",10000,0,null,null);
    for(var p:found.places().stream().filter(p->!usedVenues.contains(p.id())).limit(12).toList()){
     String id=UUID.randomUUID().toString();var s=venue(id,p,60,null,"NONE","");candidates.put(id,s);evidence.put(id,Map.of("candidateId",id,"place",p,"required",false,"lockedDuration",false));
    }
   }
   if(candidates.isEmpty())throw bad("No real place candidates were found for "+day.area()+". Add specific places or try a nearby area.");
   Map<String,Object> context=new LinkedHashMap<>();context.put("day",day);context.put("startingLocation",input.startingLocation());context.put("mode",input.transportMode());context.put("pace",request.pace());context.put("preferences",request.preferences());context.put("requestedMeals",request.meals());context.put("candidates",evidence.values());context.put("previousDays",days.stream().map(d->Map.of("date",d.date(),"area",d.area(),"visits",d.stops().stream().map(t->t.title()).toList())).toList());context.put("travelers",input.trip().travelers());context.put("budgetLkr",input.trip().budget());
   var advice=advisor.advise(context);var stops=ground(advice,candidates,required);stops.stream().filter(s->"GEOAPIFY".equals(s.placeSource())).map(ManualPlanRequest.Stop::placeId).forEach(usedVenues::add);explain(advice,explanations);
   // Preserve user-specified meal times; otherwise use editable local-clock defaults inside the day.
   for(String meal:new LinkedHashSet<>(request.meals())){
    if(stops.stream().anyMatch(s->meal.equals(s.mealType())))continue;
    var existing=day.stops().stream().filter(s->meal.equals(s.mealType())).findFirst();
    LocalTime at=existing.map(ManualPlanRequest.Stop::preferredTime).orElse(null);
    if(at==null)at=LocalTime.parse(switch(meal){case "BREAKFAST"->"08:30";case "LUNCH"->"12:30";default->"19:00";});
    if(at.isBefore(day.startTime())||!at.plusMinutes(45).isAfter(at)||at.plusMinutes(45).isAfter(day.endTime())){warnings.add(meal+" on "+day.date()+" falls outside daily hours. It was omitted; extend daily hours if you want it included.");continue;}
    stops.add(existing.orElse(new ManualPlanRequest.Stop(UUID.randomUUID().toString(),meal+" venue pending","RESTAURANT","CUSTOM",null,null,null,null,45,at,BigDecimal.ZERO,"Select a route-based venue.",meal)));
   }
   // Existing meal placeholders are retained even when the additional-meals selector is empty.
   for(var s:day.stops())if(!"NONE".equals(s.mealType())&&stops.stream().noneMatch(t->t.mealType().equals(s.mealType())))stops.add(s);
   days.add(new ManualPlanRequest.Day(day.date(),day.area(),day.startTime(),day.endTime(),List.copyOf(stops)));
  }
  var plan=copy(input,days);validate(plan);
  // Discover venues at the estimated position at each meal time, then recheck the full detour.
  for(int i=0;i<days.size();i++){
   var day=days.get(i);var options=nearby.meals(new PlanMealOptionsRequest(plan,day.date()));
   for(var meal:options.meals()){
    if(meal.suggestions()==null||meal.suggestions().options().isEmpty()){warnings.add(meal.mealType()+" on "+day.date()+": "+(meal.error()==null?"No nearby restaurant candidates found; select a venue manually.":meal.error()));continue;}
    Map<String,ManualPlanRequest.Stop> candidates=new LinkedHashMap<>();List<Object> evidence=new ArrayList<>();
    for(var option:meal.suggestions().options()) {var p=option.place();candidates.put(p.id(),venue(meal.key(),p,45,meal.at(),meal.mealType(),""));evidence.add(Map.of("candidateId",p.id(),"place",p,"detourMinutes",option.travelMinutes()));}
    var advice=advisor.advise(Map.of("task","Select exactly one restaurant for "+meal.mealType(),"at",meal.at(),"routeContext",meal.suggestions().context(),"preferences",request.preferences(),"pace",request.pace(),"candidates",evidence));
    var selected=ground(advice,candidates,Set.of());if(selected.size()!=1)throw bad("AI must select exactly one grounded meal venue. Retry planning.");
    var replacement=selected.getFirst();List<ManualPlanRequest.Stop> updated=new ArrayList<>();for(var s:days.get(i).stops())updated.add(s.key().equals(meal.key())?replacement:s);
    var d=days.get(i);days.set(i,new ManualPlanRequest.Day(d.date(),d.area(),d.startTime(),d.endTime(),List.copyOf(updated)));plan=copy(input,days);explain(advice,explanations);
   }
  // Resolve an overnight stop before querying meal locations on the following day.
  if(request.includeHotels()&&i<days.size()-1){
   var hotelDay=days.get(i);if(hotelDay.stops().stream().anyMatch(s->"HOTEL".equals(s.category())))continue;
   var at=hotelDay.endTime().minusMinutes(30);
   if(Duration.between(hotelDay.startTime(),hotelDay.endTime()).toMinutes()<30){warnings.add("No room for hotel check-in on "+hotelDay.date());continue;}
   var found=nearby.options(new PlanNearbyRequest(plan,hotelDay.date(),at,"HOTEL"));
   if(found.options().isEmpty()){warnings.add("No reachable hotel was found for "+hotelDay.date()+". Arrange accommodation before relying on the next day's origin.");continue;}
   Map<String,ManualPlanRequest.Stop> candidates=new LinkedHashMap<>();
   for(var option:found.options()){
    var p=option.place();candidates.put(p.id(),new ManualPlanRequest.Stop(UUID.randomUUID().toString(),p.name(),"HOTEL","GEOAPIFY",p.id(),p.address(),p.latitude(),p.longitude(),30,at,BigDecimal.ZERO,"Hotel check-in only; price and availability unverified.","NONE"));
   }
   var advice=advisor.advise(Map.of("task","Select exactly one overnight hotel from these candidates near the predicted endpoint. Keep the 30-minute check-in appointment locked. Do not claim availability or a booking.","date",hotelDay.date(),"at",at,"preferences",request.preferences(),"routeContext",found.context(),"candidates",found.options().stream().map(o->Map.of("candidateId",o.place().id(),"place",o.place(),"detourMinutes",o.travelMinutes(),"lockedDuration",true)).toList()));
   var chosen=ground(advice,candidates,Set.of());if(chosen.size()!=1)throw bad("AI must select exactly one grounded hotel. Retry planning.");
   var stops=new ArrayList<>(hotelDay.stops());stops.add(chosen.getFirst());days.set(i,new ManualPlanRequest.Day(hotelDay.date(),hotelDay.area(),hotelDay.startTime(),hotelDay.endTime(),List.copyOf(stops)));plan=copy(input,days);explain(advice,explanations);
  }
  }
  validate(plan);var preview=planner.preview(plan);
  if(!preview.unscheduled().isEmpty()){
   // A bounded repair can change ordering only; it cannot squeeze visits or discard selected stops.
   List<ManualPlanRequest.Day> repaired=new ArrayList<>();
   for(var d:plan.days()){
    if(preview.unscheduled().stream().noneMatch(u->u.date().equals(d.date()))||d.stops().stream().anyMatch(t->t.latitude()==null)){repaired.add(d);continue;}
    Map<String,ManualPlanRequest.Stop> candidates=new LinkedHashMap<>();d.stops().forEach(t->candidates.put(t.key(),t));
    try{
     var advice=advisor.advise(Map.of("task","Repair visit order only; all durations and fixed appointments are locked.","day",d,"mode",plan.transportMode(),"pace",request.pace(),"preferences",request.preferences(),"candidates",candidates.entrySet().stream().map(e->Map.of("candidateId",e.getKey(),"place",e.getValue(),"required",true,"lockedDuration",true)).toList(),"routeFeedback",Map.of("unscheduled",preview.unscheduled(),"legs",preview.days().stream().flatMap(pd->pd.travelLegs().stream()).map(l->Map.of("from",l.from(),"to",l.to(),"minutes",l.route().minutes())).toList())));
     ground(advice,candidates,candidates.keySet());List<ManualPlanRequest.Stop> ordered=advice.visits().stream().map(c->candidates.get(c.candidateId())).toList();
     repaired.add(new ManualPlanRequest.Day(d.date(),d.area(),d.startTime(),d.endTime(),ordered));
    }catch(ResponseStatusException ex){warnings.add("AI could not improve the order for "+d.date()+". The verified draft was kept.");repaired.add(d);}
   }
   var alternative=copy(input,repaired);var checked=planner.preview(alternative);
   if(checked.unscheduled().size()<preview.unscheduled().size()){plan=alternative;preview=checked;explanations.add("A second ordering pass reduced conflicts using real route feedback without shortening visits.");}
  }
  if(!preview.unscheduled().isEmpty())warnings.add("Some visits or meals do not fit with real travel times. They are listed below and saving is blocked. Adjust the day instead of assuming shorter visits.");
  String id=UUID.randomUUID().toString();var response=new AiPlanResponse(id,plan,preview,List.copyOf(explanations),List.copyOf(warnings),advisor.model());
  drafts.entrySet().removeIf(e->e.getValue().expires().isBefore(Instant.now()));if(drafts.size()>=100)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many pending AI drafts. Try again later.");
  drafts.put(id,new Draft(owner,Instant.now().plusSeconds(3600),response));return response;
 }
 public TripResponse save(Long owner,String id){
  var draft=drafts.get(id);if(draft==null||!draft.owner().equals(owner)||draft.expires().isBefore(Instant.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"AI draft expired or not found. Generate a new draft.");
  synchronized(draft){if(drafts.get(id)!=draft)throw new ResponseStatusException(HttpStatus.CONFLICT,"This AI draft has already been saved.");var preview=planner.preview(draft.response().plan());if(!preview.unscheduled().isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"The route has conflicts. Adjust and preview before saving.");var saved=writer.saveAi(owner,draft.response().plan(),preview,draft.response());drafts.remove(id);return saved;}
 }
 static List<ManualPlanRequest.Stop> ground(AiScheduleAdvisor.Advice advice,Map<String,ManualPlanRequest.Stop> candidates,Set<String> required){
  if(advice==null||advice.visits()==null||advice.visits().isEmpty()||advice.visits().size()>20)throw bad("AI returned an empty or oversized itinerary. Retry planning.");
  List<ManualPlanRequest.Stop> stops=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(var choice:advice.visits()){
   if(choice==null||!candidates.containsKey(choice.candidateId())||!seen.add(choice.candidateId())||choice.reason()==null||choice.reason().isBlank()||choice.reason().length()>1200||((candidates.get(choice.candidateId()).preferredTime()==null||!"NONE".equals(candidates.get(choice.candidateId()).mealType()))&&(choice.durationMinutes()<15||choice.durationMinutes()>480)))throw bad("AI returned an invalid venue or visit duration. Retry planning.");
   var s=candidates.get(choice.candidateId());int duration=s.preferredTime()!=null&&"NONE".equals(s.mealType())?s.durationMinutes():choice.durationMinutes();
   if(!"NONE".equals(s.mealType())&&(duration<30||duration>120))throw bad("AI returned an invalid meal duration. Retry planning.");
   String notes=s.notes()+"\nAI duration estimate: "+choice.reason()+" Confirm venue hours and visit requirements.";
   stops.add(new ManualPlanRequest.Stop(s.key(),s.title(),s.category(),s.placeSource(),s.placeId(),s.address(),s.latitude(),s.longitude(),duration,s.preferredTime(),s.estimatedCost(),notes,s.mealType()));
  }
  if(!seen.containsAll(required))throw bad("AI omitted a selected place. Your selections have been preserved; retry planning.");return stops;
 }
 private static void explain(AiScheduleAdvisor.Advice advice,List<String> output){if(advice.assumptions()!=null)advice.assumptions().stream().filter(Objects::nonNull).limit(10).map(s->s.substring(0,Math.min(1000,s.length()))).forEach(output::add);}
 private void validate(ManualPlanRequest plan){if(!validator.validate(plan).isEmpty())throw bad("Generated plan failed validation. Check trip dates, selections and daily hours.");}
 private static ManualPlanRequest copy(ManualPlanRequest in,List<ManualPlanRequest.Day> days){return new ManualPlanRequest(in.trip(),in.startingLocation(),in.transportMode(),in.transferMinutes(),List.copyOf(days),in.startingLatitude(),in.startingLongitude(),true);}
 private static ManualPlanRequest.Stop venue(String key,GeoPlace p,int minutes,LocalTime at,String meal,String notes){return new ManualPlanRequest.Stop(key,p.name(),"NONE".equals(meal)?"ATTRACTION":"RESTAURANT","GEOAPIFY",p.id(),p.address(),p.latitude(),p.longitude(),minutes,at,BigDecimal.ZERO,notes+"\nProvider opening hours: "+(p.openingHours()==null?"unknown; confirm with venue":p.openingHours())+". Prices and dietary suitability unverified.",meal);}
 private static ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,text);}
}
