package com.tourmate.service;
import com.tourmate.dto.*;
import com.tourmate.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PlanLocationServiceTest {
 private ManualPlanRequest.Stop stop(Double lat,Double lon){return new ManualPlanRequest.Stop("one","Selected venue","ATTRACTION","GEOAPIFY","place-id",null,lat,lon,60,null,BigDecimal.ZERO,"","NONE");}
 @Test void clientPinsCannotPoisonSharedProviderCoordinateCache(){var geo=mock(GeoapifyService.class);var service=new PlanLocationService(geo,mock(RapidTravelService.class),mock(PlaceRepository.class));service.resolve(stop(7.0,80.0));when(geo.detail("place-id")).thenReturn(new GeoPlace("place-id","Venue","London",51.5,-0.1,List.of(),null,null,null,null,null,null));assertEquals(51.5,service.resolve(stop(null,null)).latitude());service.resolve(stop(null,null));verify(geo,times(1)).detail("place-id");}
 @Test void nameOnlyCustomActivitiesRequireAPinInsteadOfGeocodingAGuess(){var service=new PlanLocationService(mock(GeoapifyService.class),mock(RapidTravelService.class),mock(PlaceRepository.class));var custom=new ManualPlanRequest.Stop("one","Lunch stop","RESTAURANT","CUSTOM",null,null,null,null,60,null,BigDecimal.ZERO,"","NONE");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.resolve(custom));}
}
