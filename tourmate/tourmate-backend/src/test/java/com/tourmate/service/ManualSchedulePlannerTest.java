package com.tourmate.service;
import com.tourmate.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class ManualSchedulePlannerTest {
 private final LocalDate date=LocalDate.of(2027,1,1);private PlanLocationService locations;private GeoRoutingService routing;private ManualSchedulePlanner planner;
 private final RoutePoint origin=new RoutePoint(0,0,"Home");
 @BeforeEach void setup(){locations=mock(PlanLocationService.class);routing=mock(GeoRoutingService.class);planner=new ManualSchedulePlanner(locations,routing);when(locations.origin(any())).thenReturn(origin);when(locations.resolve(any())).thenAnswer(i->{ManualPlanRequest.Stop s=i.getArgument(0);return new ManualPlanRequest.Stop(s.key(),s.title(),s.category(),s.placeSource(),s.placeId(),s.address(),1.0,1.0,s.durationMinutes(),s.preferredTime(),s.estimatedCost(),s.notes(),s.mealType());});when(routing.route(any(),any(),anyString())).thenAnswer(i->{RoutePoint a=i.getArgument(0),b=i.getArgument(1);int minutes="WALKING".equals(i.getArgument(2))?60:10;return new RouteEstimate(minutes,1000,List.of(a,b),List.of(0.0,minutes*60.0));});}
 private ManualPlanRequest.Stop stop(String name,int duration,String time,String meal){return new ManualPlanRequest.Stop(name,name,"NONE".equals(meal)?"ATTRACTION":"RESTAURANT","CUSTOM",null,null,1.0,1.0,duration,time==null?null:LocalTime.parse(time),new BigDecimal("100"),"Notes",meal);}
 private ManualPlanRequest plan(List<ManualPlanRequest.Stop> stops){return new ManualPlanRequest(new TripRequest("Trip",date,date,new BigDecimal("1000"),2,Set.of(),"DRAFT"),"Home","CAR",5,List.of(new ManualPlanRequest.Day(date,"Area",LocalTime.of(8,0),LocalTime.of(18,0),stops)));}
 @Test void includesTravelFromTheActualOriginAndReservesFixedMeals(){var result=planner.preview(plan(List.of(stop("Bridge",90,null,"NONE"),stop("Lunch",60,"12:30","LUNCH"))));assertTrue(result.unscheduled().isEmpty());assertEquals(LocalTime.of(8,10),result.days().getFirst().visits().getFirst().startTime());assertEquals(LocalTime.of(12,30),result.days().getFirst().visits().get(1).startTime());assertEquals(origin,result.days().getFirst().travelLegs().getFirst().from());assertEquals(LocalTime.of(8,0),result.days().getFirst().travelLegs().getFirst().departureTime());}
 @Test void travelModeChangesArrivalTimes(){var car=plan(List.of(stop("Bridge",60,null,"NONE")));var walking=new ManualPlanRequest(car.trip(),car.startingLocation(),"WALKING",car.transferMinutes(),car.days());assertEquals(LocalTime.of(8,10),planner.preview(car).days().getFirst().visits().getFirst().startTime());assertEquals(LocalTime.of(9,0),planner.preview(walking).days().getFirst().visits().getFirst().startTime());}
 @Test void rejectsAnImpossibleFixedArrivalRatherThanTeleporting(){var result=planner.preview(plan(List.of(stop("Temple",60,"08:05","NONE"))));assertEquals(1,result.unscheduled().size());assertTrue(result.days().getFirst().visits().isEmpty());assertTrue(result.unscheduled().getFirst().reason().contains("Cannot arrive"));}
 @Test void preservesTheTravelLegAfterInsertingAFlexibleVisit(){var result=planner.preview(plan(List.of(stop("Lunch",60,"10:00","LUNCH"),stop("Short stop",90,null,"NONE"))));var visits=result.days().getFirst().visits();for(int i=1;i<visits.size();i++)assertTrue(!visits.get(i).startTime().isBefore(visits.get(i-1).endTime().plusMinutes(15)));}
 @Test void carriesAnOvernightEndpointIntoTheNextDay(){var base=plan(List.of(stop("Hotel",30,"17:00","NONE")));var two=new ManualPlanRequest(new TripRequest("Trip",date,date.plusDays(1),BigDecimal.TEN,1,Set.of(),"DRAFT"),"Home","CAR",5,List.of(base.days().getFirst(),new ManualPlanRequest.Day(date.plusDays(1),"Other area",LocalTime.of(8,0),LocalTime.of(18,0),List.of(stop("Next visit",60,null,"NONE")))));var result=planner.preview(two);assertEquals("Hotel",result.days().get(1).origin().label());assertEquals("Hotel",result.days().get(1).travelLegs().getFirst().from().label());}
 @Test void unavailableTransitDoesNotFallBackToDriving(){when(routing.route(any(),any(),eq("PUBLIC_TRANSPORT"))).thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"No transit route"));var base=plan(List.of(stop("Stop",30,null,"NONE")));var transit=new ManualPlanRequest(base.trip(),"Home","PUBLIC_TRANSPORT",5,base.days());assertEquals(1,planner.preview(transit).unscheduled().size());verify(routing,never()).route(any(),any(),eq("CAR"));}
 @Test void anUnlocatedMealPlaceholderCannotBeSavedButDoesNotBlockNearbyDiscovery(){doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Choose a real venue")).when(locations).resolve(any());var placeholder=new ManualPlanRequest.Stop("meal","Lunch","RESTAURANT","CUSTOM",null,null,null,null,60,LocalTime.of(12,30),BigDecimal.ZERO,"","LUNCH");assertEquals(1,planner.preview(plan(List.of(placeholder))).unscheduled().size());assertTrue(planner.suggestionContext(plan(List.of(placeholder))).unscheduled().isEmpty());}
 @Test void reportsOverfullDays(){var result=planner.preview(plan(List.of(stop("Long visit",720,null,"NONE"),stop("Dinner",60,"19:00","DINNER"))));assertEquals(2,result.unscheduled().size());}
 @Test void optionalTravelReductionKeepsFixedMealsAndAllScheduledVisits(){
  doAnswer(i->{ManualPlanRequest.Stop s=i.getArgument(0);double coordinate=s.title().equals("A")?1:s.title().equals("B")?2:s.title().equals("C")?3:4;return new ManualPlanRequest.Stop(s.key(),s.title(),s.category(),s.placeSource(),s.placeId(),s.address(),coordinate,coordinate,s.durationMinutes(),s.preferredTime(),s.estimatedCost(),s.notes(),s.mealType());}).when(locations).resolve(any());
  doAnswer(i->{RoutePoint a=i.getArgument(0),b=i.getArgument(1);String edge=a.label()+">"+b.label();int duration=switch(edge){case "Home>A","A>B","B>C"->30;case "Home>B","B>A","A>C"->5;default->b.label().equals("Lunch")?10:50;};return new RouteEstimate(duration,1000,List.of(a,b),List.of(0.0,duration*60.0));}).when(routing).route(any(),any(),anyString());
  var base=plan(List.of(stop("A",30,null,"NONE"),stop("B",30,null,"NONE"),stop("C",30,null,"NONE"),stop("Lunch",60,"12:30","LUNCH")));
  var optimized=new ManualPlanRequest(base.trip(),base.startingLocation(),base.transportMode(),base.transferMinutes(),base.days(),null,null,true);
  var before=planner.preview(base);var after=planner.preview(optimized);
  assertTrue(after.unscheduled().isEmpty());assertEquals(before.days().getFirst().visits().size(),after.days().getFirst().visits().size());
  assertTrue(after.optimization().afterTravelMinutes()<after.optimization().beforeTravelMinutes());
  assertEquals(LocalTime.of(12,30),after.days().getFirst().visits().stream().filter(v->v.stop().mealType().equals("LUNCH")).findFirst().orElseThrow().startTime());
  assertTrue(after.optimization().description().contains("not a guarantee"));
 }
}
