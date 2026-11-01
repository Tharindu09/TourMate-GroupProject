package com.tourmate.service;

import com.tourmate.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Uses directed map-route estimates for every insertion, including travel from the starting point. */
@Service
public class ManualSchedulePlanner {
 private final PlanLocationService locations;private final GeoRoutingService routing;
 public ManualSchedulePlanner(PlanLocationService locations,GeoRoutingService routing){this.locations=locations;this.routing=routing;}
 private record Slot(ManualPlanRequest.Stop stop,int start,int end) {}
 public ManualPlanPreview preview(ManualPlanRequest input){return arrange(input,false);}
 public ManualPlanPreview suggestionContext(ManualPlanRequest input){return arrange(input,true);}
 private ManualPlanPreview arrange(ManualPlanRequest input,boolean suggestions){
  routing.providerMode(input.transportMode());
  long count=ChronoUnit.DAYS.between(input.trip().startDate(),input.trip().endDate())+1;
  if(count<1||count>60||input.days().size()!=count)throw bad("Provide daily preferences for every date of a 1–60 day trip.");
  if(input.days().stream().mapToInt(d->d.stops().size()).sum()>100)throw bad("Route planning supports up to 100 selected stops per preview. Split longer trips into smaller plans.");
  Set<LocalDate> dates=new HashSet<>();Set<String> keys=new HashSet<>();List<ManualPlanPreview.Day> output=new ArrayList<>();
  List<ManualPlanPreview.Unscheduled> unplaced=new ArrayList<>();List<String>warnings=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
  RoutePoint origin=locations.origin(input);int selections=0;int beforeTravel=0,afterTravel=0;boolean searchLimited=false;
  Map<String,RouteEstimate> requestRoutes=new HashMap<>();
  for(var day:input.days().stream().sorted(Comparator.comparing(ManualPlanRequest.Day::date)).toList()){
   if(day.date().isBefore(input.trip().startDate())||day.date().isAfter(input.trip().endDate())||!dates.add(day.date()))throw bad("Each trip date must appear exactly once.");
   int start=minutes(day.startTime()),end=minutes(day.endTime());if(start>=end)throw bad("Daily finish time must be after its start time.");
   List<ManualPlanRequest.Stop> resolved=new ArrayList<>();
   for(var stop:day.stops()){
    if(!keys.add(stop.key()))throw bad("Each stop must have a unique reference.");
    if(!"NONE".equals(stop.mealType())&&(!Set.of("RESTAURANT","HOTEL").contains(stop.category())||stop.preferredTime()==null))throw bad("Choose a time for each selected meal venue.");
    selections++;
    // Only the recommendation pass can omit an unlocated placeholder; it never produces a saveable plan.
    if(suggestions&&"CUSTOM".equals(stop.placeSource())&&(stop.latitude()==null||stop.longitude()==null)){warnings.add("Unlocated placeholder ignored for nearby discovery: "+stop.title()+". Choose a venue or pin before saving.");continue;}
    try{resolved.add(locations.resolve(stop));}catch(ResponseStatusException ex){if(ex.getStatusCode().value()==422||ex.getStatusCode().value()==404){unplaced.add(new ManualPlanPreview.Unscheduled(day.date(),stop.title(),ex.getReason()));}else throw ex;}
   }
   List<Slot> placed=new ArrayList<>();RoutePoint dayOrigin=origin;
   for(var stop:resolved.stream().filter(s->s.preferredTime()!=null).sorted(Comparator.comparing(ManualPlanRequest.Stop::preferredTime)).toList()){
    int from=minutes(stop.preferredTime()),to=from+stop.durationMinutes();if(from<start||to>end){unplaced.add(new ManualPlanPreview.Unscheduled(day.date(),stop.title(),"Preferred visit time is outside the day’s travel hours."));continue;}
    Slot previous=placed.isEmpty()?null:placed.getLast();RoutePoint point=previous==null?dayOrigin:point(previous.stop());int departure=previous==null?start:previous.end()+input.transferMinutes();
    try{var route=route(point,point(stop),input,requestRoutes);if(departure+route.minutes()>from){unplaced.add(new ManualPlanPreview.Unscheduled(day.date(),stop.title(),"Cannot arrive by "+time(from)+". The "+input.transportMode().toLowerCase()+" route takes about "+route.minutes()+" minutes from "+point.label()+". Choose a later time or another stop."));continue;}placed.add(new Slot(stop,from,to));}
    catch(ResponseStatusException ex){if(ex.getStatusCode().value()==422)unplaced.add(new ManualPlanPreview.Unscheduled(day.date(),stop.title(),ex.getReason()));else throw ex;}
   }
   for(var stop:resolved){
    if(stop.preferredTime()!=null)continue;boolean added=false;String reason="Not enough time for this visit and the actual travel legs. Shorten a visit, extend the day, or move it to another date.";
    for(int index=0;index<=placed.size();index++){
     Slot before=index==0?null:placed.get(index-1),after=index==placed.size()?null:placed.get(index);
     RoutePoint from=before==null?dayOrigin:point(before.stop());int departure=before==null?start:before.end()+input.transferMinutes();
     try{
      int arrival=departure+route(from,point(stop),input,requestRoutes).minutes(),finish=arrival+stop.durationMinutes();if(finish>end)continue;
      if(after!=null&&finish+input.transferMinutes()+route(point(stop),point(after.stop()),input,requestRoutes).minutes()>after.start())continue;
      placed.add(index,new Slot(stop,arrival,finish));added=true;break;
     }catch(ResponseStatusException ex){if(ex.getStatusCode().value()==422)reason=ex.getReason();else throw ex;}
    }
    if(!added)unplaced.add(new ManualPlanPreview.Unscheduled(day.date(),stop.title(),reason));
   }
   int baselineMinutes=travelMinutes(placed,dayOrigin,input,requestRoutes);beforeTravel+=baselineMinutes;
   if(Boolean.TRUE.equals(input.optimizeTravel())&&placed.size()>1){
    var optimized=optimize(placed,dayOrigin,start,end,input,requestRoutes);
    placed=new ArrayList<>(optimized.slots());searchLimited|=optimized.limited();
   }
   afterTravel+=travelMinutes(placed,dayOrigin,input,requestRoutes);
   List<ManualPlanPreview.TravelLeg> legs=new ArrayList<>();RoutePoint previous=dayOrigin;int departure=start;
   for(var visit:placed){var route=route(previous,point(visit.stop()),input,requestRoutes);if(route.minutes()>0)legs.add(new ManualPlanPreview.TravelLeg(previous,point(visit.stop()),time(departure),time(departure+route.minutes()),input.transportMode(),route));previous=point(visit.stop());departure=visit.end()+input.transferMinutes();total=total.add(visit.stop().estimatedCost());}
   var visits=placed.stream().map(s->new ManualPlanPreview.Visit(s.stop(),time(s.start()),time(s.end()))).toList();
   if(!placed.isEmpty())origin=point(placed.getLast().stop());
   if(day.stops().stream().noneMatch(s->!"NONE".equals(s.mealType())))warnings.add(day.date()+": no meal venue selected. Use route-based meal options to choose one.");
   output.add(new ManualPlanPreview.Day(day.date(),day.area(),visits,dayOrigin,List.copyOf(legs)));
  }
  if(selections==0&&!suggestions)throw bad("Select at least one visit or meal stop.");
  if(total.compareTo(input.trip().budget())>0)warnings.add("Estimated group cost exceeds the budget.");
  warnings.add("Geoapify/OpenStreetMap route estimates include the starting journey. Extra stop buffer: "+input.transferMinutes()+" minutes. Arrival times are estimates, not live traffic or guaranteed transit departures.");
  warnings.add("Each later day starts at the previous day's last scheduled place. Select an overnight hotel as your last stop if you will stay elsewhere. Times use one planning clock; adjust preferences for timezone changes.");
  if("PUBLIC_TRANSPORT".equals(input.transportMode()))warnings.add("Public transport is used only where the provider supplies a transit route. Verify service timetables; no driving fallback is used.");
  var optimization=new ManualPlanPreview.Optimization(Boolean.TRUE.equals(input.optimizeTravel()),beforeTravel,afterTravel,searchLimited,
   "Bounded day-by-day relocation of flexible visits using directed route times. Fixed visits and all scheduled stops are retained. This is a travel-reduction heuristic, not a guarantee of global optimality; stops that initially did not fit still need adjustment.");
  return new ManualPlanPreview(output,List.copyOf(unplaced),List.copyOf(warnings),total,input.trip().budget().subtract(total),optimization);
 }
 private record Improved(List<Slot> slots,boolean limited) {}
 private static class SearchLimit extends RuntimeException {}
 private Improved optimize(List<Slot> baseline,RoutePoint origin,int start,int end,ManualPlanRequest input,Map<String,RouteEstimate> routes){
  List<Slot> best=List.copyOf(baseline);int score=travelMinutes(best,origin,input,routes);int allowance=Math.min(190,routes.size()+40);boolean limited=false;
  try{
   for(int pass=0;pass<2;pass++){
    boolean changed=false;
    for(int i=0;i<best.size();i++){
     var selected=best.get(i).stop();if(selected.preferredTime()!=null||"HOTEL".equals(selected.category()))continue;
     for(int target=0;target<best.size();target++){
      if(i==target)continue;var order=new ArrayList<>(best.stream().map(Slot::stop).toList());order.remove(i);order.add(target,selected);
      // Keep an overnight hotel as the final stop rather than changing the next day's starting location.
      if("HOTEL".equals(best.getLast().stop().category())&&!order.getLast().key().equals(best.getLast().stop().key()))continue;
      var trial=reschedule(order,origin,start,end,input,routes,allowance);if(trial==null)continue;
      int candidate=travelMinutes(trial,origin,input,routes);
      if(candidate<score){best=trial;score=candidate;changed=true;break;}
     }
     if(changed)break;
    }
    if(!changed)break;
   }
  }catch(SearchLimit ex){limited=true;}
  catch(ResponseStatusException ex){if(ex.getStatusCode().value()==429||ex.getStatusCode().value()>=500)limited=true;else throw ex;}
  return new Improved(best,limited);
 }
 private List<Slot> reschedule(List<ManualPlanRequest.Stop> order,RoutePoint origin,int start,int end,ManualPlanRequest input,Map<String,RouteEstimate> routes,int allowance){
  List<Slot> result=new ArrayList<>();RoutePoint from=origin;int departure=start;
  for(var stop:order){
   String key=from.latitude()+","+from.longitude()+":"+stop.latitude()+","+stop.longitude();
   if(!routes.containsKey(key)&&routes.size()>=allowance)throw new SearchLimit();
   RouteEstimate route;try{route=route(from,point(stop),input,routes);}catch(ResponseStatusException ex){if(ex.getStatusCode().value()==422)return null;throw ex;}
   int arrival=departure+route.minutes();int at=stop.preferredTime()==null?arrival:minutes(stop.preferredTime());int finish=at+stop.durationMinutes();
   if(arrival>at||finish>end)return null;result.add(new Slot(stop,at,finish));from=point(stop);departure=finish+input.transferMinutes();
  }
  return List.copyOf(result);
 }
 private int travelMinutes(List<Slot> slots,RoutePoint origin,ManualPlanRequest input,Map<String,RouteEstimate> routes){int total=0;RoutePoint from=origin;for(var slot:slots){total+=route(from,point(slot.stop()),input,routes).minutes();from=point(slot.stop());}return total;}
 private RouteEstimate route(RoutePoint a,RoutePoint b,ManualPlanRequest input,Map<String,RouteEstimate> routes){String key=a.latitude()+","+a.longitude()+":"+b.latitude()+","+b.longitude();if(!routes.containsKey(key)){if(routes.size()>=200)throw bad("This plan needs too many route combinations. Reduce the number of stops.");routes.put(key,routing.route(a,b,input.transportMode()));}return routes.get(key);}
 private RoutePoint point(ManualPlanRequest.Stop s){return new RoutePoint(s.latitude(),s.longitude(),s.title());}
 private int minutes(LocalTime time){if(time.getSecond()!=0||time.getNano()!=0)throw bad("Use times with minute precision.");return time.getHour()*60+time.getMinute();}
 private LocalTime time(int minutes){return LocalTime.of(minutes/60,minutes%60);}
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
