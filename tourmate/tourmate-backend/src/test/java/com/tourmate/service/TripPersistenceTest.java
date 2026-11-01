package com.tourmate.service;

import com.tourmate.dto.*;
import com.tourmate.model.*;
import com.tourmate.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:triptest;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
 "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type=TIMESTAMP", "logging.level.root=ERROR", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(TripService.class)
class TripPersistenceTest {
 @Autowired TripService service;
 @Autowired UserRepository users;
 @Autowired TripRepository trips;
 @Autowired TripDayRepository days;
 @Autowired TripItemRepository items;
 private Long owner;
 private Long stranger;
 @BeforeEach void setup() {
  owner=user("owner");stranger=user("stranger");
 }
 private Long user(String prefix) {User u=new User();u.firstName="Test";u.lastName="User";u.email=prefix+UUID.randomUUID()+"@example.com";u.passwordHash="test-hash";return users.saveAndFlush(u).id;}
 private TripRequest body(LocalDate start,LocalDate end) {return new TripRequest(" Ella adventure ",start,end,new BigDecimal("10000.00"),2,Set.of(1L),"DRAFT");}
 private TripResponse trip() {return service.create(owner,body(LocalDate.of(2027,1,1),LocalDate.of(2027,1,3)));}
 private TripItemRequest activity(Long day,String title,String start,String end,String cost) {return new TripItemRequest(day,title,"ACTIVITY","CUSTOM",null,null,null,null,LocalTime.parse(start),LocalTime.parse(end),new BigDecimal(cost),"Bring water");}
 @Test void createsAllDaysAndPersistsAnOwnedTripWithDestinations() {
  var trip=trip();assertEquals("Ella adventure",trip.name());assertEquals(3,trip.days().size());assertEquals(Set.of(1L),trip.destinationIds());
  assertEquals(LocalDate.of(2027,1,3),trip.days().get(2).date());assertEquals(3,trip.days().get(2).dayNumber());
  assertEquals(1,service.list(owner,0,12).totalElements());assertEquals(0,service.list(stranger,0,12).totalElements());
  assertEquals(trip.id(),service.detail(owner,trip.id()).id());
 }
 @Test void rejectsOtherUsersReadsUpdatesDeletesAndItemWrites() {
  var trip=trip();Long day=trip.days().getFirst().id();
  assertEquals(404,assertThrows(ResponseStatusException.class,()->service.detail(stranger,trip.id())).getStatusCode().value());
  assertThrows(ResponseStatusException.class,()->service.update(stranger,trip.id(),body(trip.startDate(),trip.endDate())));
  assertThrows(ResponseStatusException.class,()->service.delete(stranger,trip.id()));
  assertThrows(ResponseStatusException.class,()->service.addItem(stranger,trip.id(),activity(day,"Private","09:00","10:00","0")));
  assertTrue(trips.existsById(trip.id()));
 }
 @Test void savesCustomActivitiesCalculatesBudgetAndRejectsOverlaps() {
  var trip=trip();Long day=trip.days().getFirst().id();
  var result=service.addItem(owner,trip.id(),activity(day,"Walk","09:00","10:00","1250.50"));
  assertEquals(0,new BigDecimal("1250.50").compareTo(result.estimatedTotal()));assertEquals(0,new BigDecimal("8749.50").compareTo(result.remainingBudget()));
  assertEquals(0,result.estimatedTotal().compareTo(result.costByCategory().get("ACTIVITY")));
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.addItem(owner,trip.id(),activity(day,"Overlap","09:30","10:30","0"))).getStatusCode().value());
  assertThrows(ResponseStatusException.class,()->service.addItem(owner,trip.id(),activity(day,"Backwards","12:00","11:00","0")));
  var adjacent=service.addItem(owner,trip.id(),activity(day,"Lunch","10:00","11:00","100"));assertEquals(2,adjacent.days().getFirst().items().size());
 }
 @Test void movesReordersEditsAndDeletesActivitiesWithoutLosingTheirData() {
  var trip=trip();Long d1=trip.days().get(0).id(),d2=trip.days().get(1).id();
  var first=service.addItem(owner,trip.id(),activity(d1,"Walk","09:00","10:00","500")).days().getFirst().items().getFirst();
  var second=service.addItem(owner,trip.id(),activity(d1,"Cafe","11:00","12:00","200")).days().getFirst().items().get(1);
  var ordered=service.reorder(owner,trip.id(),d1,List.of(second.id(),first.id()));assertEquals(second.id(),ordered.days().getFirst().items().getFirst().id());
  assertThrows(ResponseStatusException.class,()->service.reorder(owner,trip.id(),d1,List.of(first.id(),first.id())));
  var moved=service.move(owner,trip.id(),first.id(),d2,0);assertEquals(first.id(),moved.days().get(1).items().getFirst().id());assertEquals("Bring water",moved.days().get(1).items().getFirst().notes());
  var edited=service.updateItem(owner,trip.id(),first.id(),activity(d2,"Longer walk","08:30","10:30","700"));assertEquals("Longer walk",edited.days().get(1).items().getFirst().title());
  var removed=service.deleteItem(owner,trip.id(),second.id());assertTrue(removed.days().getFirst().items().isEmpty());assertEquals(0,new BigDecimal("700").compareTo(removed.estimatedTotal()));
 }
 @Test void refusesOverlappingMovesAndDaysFromAnotherTrip() {
  var trip=trip();Long d1=trip.days().get(0).id(),d2=trip.days().get(1).id();
  var first=service.addItem(owner,trip.id(),activity(d1,"Walk","09:00","10:00","0")).days().getFirst().items().getFirst();
  service.addItem(owner,trip.id(),activity(d2,"Cafe","09:00","10:00","0"));
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.move(owner,trip.id(),first.id(),d2,0)).getStatusCode().value());
  var other=trip();assertThrows(ResponseStatusException.class,()->service.move(owner,trip.id(),first.id(),other.days().getFirst().id(),0));
  assertThrows(ResponseStatusException.class,()->service.addItem(owner,trip.id(),activity(other.days().getFirst().id(),"Foreign day","12:00","13:00","0")));
 }
 @Test void dateChangesPreserveExistingDaysAndDoNotSilentlyDeleteActivities() {
  var trip=trip();Long day=trip.days().getFirst().id();service.addItem(owner,trip.id(),activity(day,"Walk","09:00","10:00","0"));
  assertThrows(ResponseStatusException.class,()->service.update(owner,trip.id(),body(trip.startDate().plusDays(1),trip.endDate())));
  var extended=service.update(owner,trip.id(),body(trip.startDate(),trip.endDate().plusDays(1)));
  assertEquals(4,extended.days().size());assertEquals(day,extended.days().getFirst().id());assertEquals(1,extended.days().getFirst().items().size());
 }
 @Test void localPlacesUseCanonicalCoordinatesAndDeletionCascadesToAllDaysAndItems() {
  var trip=trip();Long day=trip.days().getFirst().id();
  var req=new TripItemRequest(day,"Bridge","ATTRACTION","TOURMATE","1","not canonical",7.0,80.0,LocalTime.of(9,0),LocalTime.of(10,0),BigDecimal.ZERO,"");
  var result=service.addItem(owner,trip.id(),req);assertEquals(6.8768,result.days().getFirst().items().getFirst().latitude());
  service.delete(owner,trip.id());trips.flush();assertFalse(trips.existsById(trip.id()));assertTrue(days.findByTripIdOrderByDateAsc(trip.id()).isEmpty());assertTrue(items.findByDayIdOrderByPositionAscIdAsc(day).isEmpty());
 }
 @Test void globalCoordinatesSurviveTheDatabaseConstraintMigration() {
  var trip=trip();Long day=trip.days().getFirst().id();
  var req=new TripItemRequest(day,"Eiffel Tower","ATTRACTION","GEOAPIFY","provider-place","Paris, France",48.8584,2.2945,LocalTime.of(9,0),LocalTime.of(10,0),BigDecimal.ZERO,"");
  var result=service.addItem(owner,trip.id(),req);items.flush();
  assertEquals(48.8584,result.days().getFirst().items().getFirst().latitude());
  assertEquals(2.2945,result.days().getFirst().items().getFirst().longitude());
 }
 @Test void rejectsBadDateRangesAndUnknownDestinations() {
  assertThrows(ResponseStatusException.class,()->service.create(owner,body(LocalDate.of(2027,1,2),LocalDate.of(2027,1,1))));
  assertThrows(ResponseStatusException.class,()->service.create(owner,body(LocalDate.of(2027,1,1),LocalDate.of(2027,4,1))));
  assertThrows(ResponseStatusException.class,()->service.create(owner,new TripRequest("Trip",LocalDate.of(2027,1,1),LocalDate.of(2027,1,1),BigDecimal.ZERO,1,Set.of(999999L),"DRAFT")));
 }
}
