package com.tourmate.service;
import com.tourmate.dto.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AiTripPlanServiceTest {
 AiScheduleAdvisor advisor=mock(AiScheduleAdvisor.class);GeoapifyService places=mock(GeoapifyService.class);
 ManualSchedulePlanner planner=mock(ManualSchedulePlanner.class);PlanNearbyService nearby=mock(PlanNearbyService.class);TripPlanWriter writer=mock(TripPlanWriter.class);
 jakarta.validation.ValidatorFactory factory;AiTripPlanService service;
 final LocalDate date=LocalDate.of(2027,2,1);
 @BeforeEach void setup(){factory=Validation.buildDefaultValidatorFactory();service=new AiTripPlanService(advisor,places,planner,nearby,writer,factory.getValidator());when(advisor.model()).thenReturn("test-model");when(nearby.meals(any())).thenReturn(new PlanMealOptionsResponse(List.of()));when(planner.preview(any())).thenReturn(preview(List.of()));}
 @AfterEach void close(){factory.close();}
 ManualPlanPreview preview(List<ManualPlanPreview.Unscheduled> unfit){return new ManualPlanPreview(List.of(),unfit,List.of(),BigDecimal.ZERO,BigDecimal.TEN);}
 ManualPlanRequest.Stop stop(String key,int duration,LocalTime at,String meal){return new ManualPlanRequest.Stop(key,"Large botanical garden","NONE".equals(meal)?"ATTRACTION":"RESTAURANT","CUSTOM",null,"Address",51.1,-0.1,duration,at,BigDecimal.ZERO,"User note",meal);}
 ManualPlanRequest plan(List<ManualPlanRequest.Stop> stops){return new ManualPlanRequest(new TripRequest("AI trip",date,date,BigDecimal.TEN,2,Set.of(),"DRAFT"),"Hotel","WALKING",10,List.of(new ManualPlanRequest.Day(date,"Any region",LocalTime.of(8,0),LocalTime.of(20,0),stops)),51.0,-0.1,true);}
 AiScheduleAdvisor.Advice advice(String id,int duration){return new AiScheduleAdvisor.Advice(List.of(new AiScheduleAdvisor.Choice(id,duration,"Allow time to explore the extensive gardens at a relaxed pace; size is unverified.")),List.of("Confirm opening hours."));}
 @Test void keepsIntelligentLongDurationAndUserNotesRatherThanBlanketOneHour(){var p=stop("garden",60,null,"NONE");var result=AiTripPlanService.ground(advice("garden",180),Map.of("garden",p),Set.of("garden"));assertEquals(180,result.getFirst().durationMinutes());assertTrue(result.getFirst().notes().contains("User note"));assertTrue(result.getFirst().notes().contains("extensive gardens"));}
 @Test void neverReplacesExplicitAppointmentDuration(){var p=stop("garden",600,LocalTime.of(9,0),"NONE");assertEquals(600,AiTripPlanService.ground(advice("garden",600),Map.of("garden",p),Set.of("garden")).getFirst().durationMinutes());}
 @Test void rejectsInventedVenueAndMissingRequiredVisitAndDuplicate(){var p=stop("garden",60,null,"NONE");assertThrows(ResponseStatusException.class,()->AiTripPlanService.ground(advice("made-up",180),Map.of("garden",p),Set.of()));assertThrows(ResponseStatusException.class,()->AiTripPlanService.ground(advice("garden",180),Map.of("garden",p),Set.of("other")));var duplicate=new AiScheduleAdvisor.Advice(List.of(advice("garden",180).visits().getFirst(),advice("garden",180).visits().getFirst()),List.of());assertThrows(ResponseStatusException.class,()->AiTripPlanService.ground(duplicate,Map.of("garden",p),Set.of()));}
 @Test void rejectsUnusableMealDuration(){assertThrows(ResponseStatusException.class,()->AiTripPlanService.ground(advice("meal",240),Map.of("meal",stop("meal",45,LocalTime.NOON,"LUNCH")),Set.of()));}
 @Test void serverOwnedDraftCannotBeSavedByAnotherUserAndRoutesRechecked(){when(advisor.advise(any())).thenReturn(advice("garden",180));var result=service.generate(1L,new AiPlanRequest(plan(List.of(stop("garden",60,null,"NONE"))),"Gardens and a relaxed pace","RELAXED",List.of()));assertThrows(ResponseStatusException.class,()->service.save(2L,result.draftId()));when(planner.preview(any())).thenReturn(preview(List.of(new ManualPlanPreview.Unscheduled(date,"Garden","Route no longer fits"))));assertEquals(409,assertThrows(ResponseStatusException.class,()->service.save(1L,result.draftId())).getStatusCode().value());verifyNoInteractions(writer);}
 @Test void missingModelFailsBeforeSpendingPlaceProviderQuota(){doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Install model")).when(advisor).checkAvailable();assertThrows(ResponseStatusException.class,()->service.generate(1L,new AiPlanRequest(plan(List.of()),"Culture","BALANCED",List.of())));verifyNoInteractions(places,planner);}
 @Test void mealChoiceIsGroundedInRouteContextAndItsFullDetourIsValidated(){
  var cafe=new GeoPlace("cafe","Route cafe","Street",51.2,-0.2,List.of("catering.restaurant"),null,null,null,null,null,null);
  when(advisor.advise(any())).thenAnswer(i->{Object context=i.getArgument(0);if(context instanceof Map<?,?> m&&m.containsKey("task"))return advice("cafe",45);return advice("garden",180);});
  when(nearby.meals(any())).thenAnswer(i->{PlanMealOptionsRequest r=i.getArgument(0);var meal=r.plan().days().getFirst().stops().stream().filter(s->s.mealType().equals("LUNCH")).findFirst().orElseThrow();return new PlanMealOptionsResponse(List.of(new PlanMealOptionsResponse.Meal(meal.key(),"LUNCH",LocalTime.of(12,30),new PlanNearbyResponse(new RoutePoint(51.2,-0.2,"On route"),LocalTime.of(12,30),"Near the garden","WALKING",List.of(new PlanNearbyResponse.Option(cafe,7,500)),List.of()),null)));});
  var result=service.generate(1L,new AiPlanRequest(plan(List.of(stop("garden",60,null,"NONE"))),"Vegetarian options if verified","RELAXED",List.of("LUNCH")));
  var lunch=result.plan().days().getFirst().stops().stream().filter(s->s.mealType().equals("LUNCH")).findFirst().orElseThrow();assertEquals("cafe",lunch.placeId());assertEquals(51.2,lunch.latitude());assertEquals(LocalTime.of(12,30),lunch.preferredTime());verify(planner).preview(result.plan());assertTrue(result.warnings().stream().anyMatch(w->w.contains("dietary")));
 }
 @Test void automaticallyChoosesHotelAndUsesItWhenQueryingNextDayMeals(){
  var first=plan(List.of(stop("garden",60,null,"NONE")));var second=date.plusDays(1);
  var input=new ManualPlanRequest(new TripRequest("Two days",date,second,BigDecimal.TEN,2,Set.of(),"DRAFT"),first.startingLocation(),first.transportMode(),first.transferMinutes(),List.of(first.days().getFirst(),new ManualPlanRequest.Day(second,"Next area",LocalTime.of(8,0),LocalTime.of(20,0),first.days().getFirst().stops())));
  var hotel=new GeoPlace("hotel","Real overnight hotel","Road",51.2,-0.2,List.of("accommodation.hotel"),null,null,null,null,null,null);
  when(nearby.options(any())).thenAnswer(i->{PlanNearbyRequest r=i.getArgument(0);assertEquals(date,r.date());assertEquals("HOTEL",r.category());assertEquals(LocalTime.of(19,30),r.at());return new PlanNearbyResponse(new RoutePoint(51.2,-0.2,"Endpoint"),r.at(),"At the estimated endpoint","WALKING",List.of(new PlanNearbyResponse.Option(hotel,5,200)),List.of());});
  when(advisor.advise(any())).thenAnswer(i->{Map<?,?> context=i.getArgument(0);return context.containsKey("task")?advice("hotel",30):advice("garden",180);});
  var checked=new java.util.concurrent.atomic.AtomicBoolean();
  when(nearby.meals(any())).thenAnswer(i->{PlanMealOptionsRequest r=i.getArgument(0);if(r.date().equals(second)){assertTrue(r.plan().days().getFirst().stops().stream().anyMatch(t->"HOTEL".equals(t.category())));checked.set(true);}return new PlanMealOptionsResponse(List.of());});
  var result=service.generate(1L,new AiPlanRequest(input,"Nature and hotels","RELAXED",List.of(),true));
  var stay=result.plan().days().getFirst().stops().stream().filter(t->"HOTEL".equals(t.category())).findFirst().orElseThrow();assertEquals("hotel",stay.placeId());assertEquals(30,stay.durationMinutes());assertTrue(checked.get());assertTrue(result.plan().days().getLast().stops().stream().noneMatch(t->"HOTEL".equals(t.category())));
 }
 @Test void omitsDefaultMealsOutsideDailyHoursWithAnExplanation(){
  when(advisor.advise(any())).thenReturn(advice("garden",180));var original=plan(List.of(stop("garden",60,null,"NONE")));var day=original.days().getFirst();
  var input=new ManualPlanRequest(original.trip(),original.startingLocation(),original.transportMode(),0,List.of(new ManualPlanRequest.Day(day.date(),day.area(),LocalTime.of(10,0),LocalTime.of(18,0),day.stops())));
  var result=service.generate(1L,new AiPlanRequest(input,"Nature","BALANCED",List.of("BREAKFAST","DINNER")));
  assertEquals(1,result.plan().days().getFirst().stops().size());assertEquals(2,result.warnings().stream().filter(w->w.contains("It was omitted")).count());
 }

 @Test void discoversRealCandidatesWhenUserOnlySuppliesBasicsAndPreferences(){
  var real=new GeoPlace("real-garden","Actual garden","Road",51.1,-0.1,List.of("tourism.attraction"),null,null,null,null,null,null);
  when(places.search(eq("Any region"),eq("ATTRACTION"),eq(""),eq(10000),eq(0),isNull(),isNull())).thenReturn(new GeoSearchResult(List.of(real),0,false,51.1,-0.1,"Any region"));
  when(advisor.advise(any())).thenAnswer(i->{Map<?,?> context=i.getArgument(0);assertEquals("Nature, accessible walking and vegetarian meals",context.get("preferences"));var candidates=(Collection<?>)context.get("candidates");var entry=(Map<?,?>)candidates.iterator().next();return advice((String)entry.get("candidateId"),120);});
  var result=service.generate(1L,new AiPlanRequest(plan(List.of()),"Nature, accessible walking and vegetarian meals","RELAXED",List.of()));
  var visit=result.plan().days().getFirst().stops().getFirst();assertEquals("real-garden",visit.placeId());assertEquals("Actual garden",visit.title());assertEquals(120,visit.durationMinutes());verify(planner).preview(result.plan());
 }

}
