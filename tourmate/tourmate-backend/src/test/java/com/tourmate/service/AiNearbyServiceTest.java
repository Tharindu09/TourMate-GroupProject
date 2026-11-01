package com.tourmate.service;
import com.tourmate.dto.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AiNearbyServiceTest {
 final PlanNearbyService nearby=mock(PlanNearbyService.class);
 final AiScheduleAdvisor advisor=mock(AiScheduleAdvisor.class);
 final AiNearbyService service=new AiNearbyService(nearby,advisor);
 final PlanNearbyRequest request=new PlanNearbyRequest(null,LocalDate.of(2027,1,1),LocalTime.of(12,30),"RESTAURANT");
 PlanNearbyResponse found(){return new PlanNearbyResponse(new RoutePoint(7.3,80.3,"On route"),request.at(),"Midway through the journey","CAR",List.of(option("a"),option("b")),List.of());}
 PlanNearbyResponse.Option option(String id){return new PlanNearbyResponse.Option(new GeoPlace(id,"Cafe "+id,"Road",7.31,80.31,List.of("catering.restaurant"),null,null,null,null,null,null),5,1000);}
 @Test void ranksOnlyRetrievedVenuesAndPreservesTheirCoordinatesAndRoutes(){
  var original=found();when(nearby.options(request)).thenReturn(original);when(advisor.model()).thenReturn("test");
  when(advisor.advise(any())).thenAnswer(call->{var context=(Map<?,?>)call.getArgument(0);assertEquals("Vegetarian lunch",context.get("preferences"));assertEquals(request.at(),context.get("at"));assertEquals(original.anchor(),context.get("anchor"));return new AiScheduleAdvisor.Advice(List.of(new AiScheduleAdvisor.Choice("b",45,"Consider this nearby option; dietary suitability unknown."),new AiScheduleAdvisor.Choice("a",45,"Another nearby option.")),List.of());});
  var ranked=service.recommend(request,"Vegetarian lunch");assertSame(original.options().get(1),ranked.nearby().options().getFirst());assertEquals(2,ranked.reasons().size());assertEquals(original.anchor(),ranked.nearby().anchor());verify(advisor).checkAvailable();
 }
 @Test void rejectsInventedDuplicateAndMissingVenues(){
  when(nearby.options(request)).thenReturn(found());
  for(var ids:List.of(List.of("a","invented"),List.of("a","a"),List.of("a"))){when(advisor.advise(any())).thenReturn(new AiScheduleAdvisor.Advice(ids.stream().map(id->new AiScheduleAdvisor.Choice(id,45,"Reason")).toList(),List.of()));assertEquals(502,assertThrows(ResponseStatusException.class,()->service.recommend(request,"Lunch")).getStatusCode().value());}
 }
 @Test void checksOllamaBeforeSpendingPlaceQuota(){
  doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Ollama unavailable")).when(advisor).checkAvailable();assertThrows(ResponseStatusException.class,()->service.recommend(request,"Lunch"));verifyNoInteractions(nearby);
 }
 @Test void emptyOptionsDoNotAskAiToInventVenues(){
  when(nearby.options(request)).thenReturn(new PlanNearbyResponse(found().anchor(),request.at(),"On route","CAR",List.of(),List.of()));assertTrue(service.recommend(request,"Lunch").nearby().options().isEmpty());verify(advisor,never()).advise(any());
 }
}
