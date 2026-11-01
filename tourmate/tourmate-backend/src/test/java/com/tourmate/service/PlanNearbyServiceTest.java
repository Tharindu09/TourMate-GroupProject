package com.tourmate.service;
import com.tourmate.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PlanNearbyServiceTest {
 @Test void restaurantsAreQueriedAtPredictedRoutePositionNotTheDestinationTown(){
  var planner=mock(ManualSchedulePlanner.class);var routing=mock(GeoRoutingService.class);var places=mock(GeoapifyService.class);LocalDate date=LocalDate.of(2027,1,1);
  var input=new ManualPlanRequest(new TripRequest("Trip",date,date,BigDecimal.ZERO,1,Set.of(),"DRAFT"),"Origin","CAR",0,List.of(new ManualPlanRequest.Day(date,"Distant destination",LocalTime.of(8,0),LocalTime.of(18,0),List.of())));
  var a=new RoutePoint(7,80,"Origin");var b=new RoutePoint(8,81,"Far destination");var midway=new RoutePoint(7.3,80.3,"On route");var route=new RouteEstimate(60,100000,List.of(a,b),List.of(0.0,3600.0));
  when(planner.suggestionContext(input)).thenReturn(new ManualPlanPreview(List.of(new ManualPlanPreview.Day(date,"Distant destination",List.of(),a,List.of(new ManualPlanPreview.TravelLeg(a,b,LocalTime.of(8,0),LocalTime.of(9,0),"CAR",route)))),List.of(),List.of(),BigDecimal.ZERO,BigDecimal.ZERO));
  when(routing.position(route,30)).thenReturn(midway);
  var cafe=new GeoPlace("cafe","Roadside cafe","Near route",7.31,80.31,List.of("catering.restaurant"),null,null,null,null,null,null);
  when(places.search(anyString(),eq("RESTAURANT"),eq(""),eq(5000),eq(0),eq(7.3),eq(80.3))).thenReturn(new GeoSearchResult(List.of(cafe),0,false,7.3,80.3,"Route"));
  when(routing.route(any(),any(),eq("CAR"))).thenReturn(new RouteEstimate(4,1500,List.of(midway,b),List.of(0.0,240.0)));
  var result=new PlanNearbyService(planner,routing,places).options(new PlanNearbyRequest(input,date,LocalTime.of(8,30),"RESTAURANT"));
  assertEquals(midway,result.anchor());assertEquals("Roadside cafe",result.options().getFirst().place().name());assertEquals(4,result.options().getFirst().travelMinutes());
  verify(places).search("Route position","RESTAURANT","",5000,0,7.3,80.3);
 }
 @Test void automaticBreakfastLunchDinnerUseOneSharedRouteContext(){
  var planner=mock(ManualSchedulePlanner.class);var routing=mock(GeoRoutingService.class);var places=mock(GeoapifyService.class);LocalDate date=LocalDate.of(2027,1,1);
  var breakfast=new ManualPlanRequest.Stop("breakfast","Breakfast","RESTAURANT","CUSTOM",null,null,null,null,30,LocalTime.of(8,0),BigDecimal.ZERO,"","BREAKFAST");
  var lunch=new ManualPlanRequest.Stop("lunch","Lunch","RESTAURANT","CUSTOM",null,null,null,null,60,LocalTime.of(12,30),BigDecimal.ZERO,"","LUNCH");
  var dinner=new ManualPlanRequest.Stop("dinner","Dinner","RESTAURANT","CUSTOM",null,null,null,null,60,LocalTime.of(19,0),BigDecimal.ZERO,"","DINNER");
  var input=new ManualPlanRequest(new TripRequest("Trip",date,date,BigDecimal.ZERO,1,Set.of(),"DRAFT"),"Home","CAR",0,List.of(new ManualPlanRequest.Day(date,"Area",LocalTime.of(8,0),LocalTime.of(20,0),List.of(breakfast,lunch,dinner))));
  var point=new RoutePoint(7,80,"Home");when(planner.suggestionContext(input)).thenReturn(new ManualPlanPreview(List.of(new ManualPlanPreview.Day(date,"Area",List.of(),point,List.of())),List.of(),List.of(),BigDecimal.ZERO,BigDecimal.ZERO));
  when(places.search(anyString(),anyString(),anyString(),anyInt(),anyInt(),anyDouble(),anyDouble())).thenReturn(new GeoSearchResult(List.of(),0,false,7,80,"Home"));
  var result=new PlanNearbyService(planner,routing,places).meals(new PlanMealOptionsRequest(input,date));
  assertEquals(3,result.meals().size());assertEquals(LocalTime.of(8,0),result.meals().get(0).at());assertEquals(LocalTime.of(12,30),result.meals().get(1).at());assertEquals(LocalTime.of(19,0),result.meals().get(2).at());
  verify(planner,times(1)).suggestionContext(input);verify(places,times(3)).search("Route position","RESTAURANT","",5000,0,7.0,80.0);
 }
}
