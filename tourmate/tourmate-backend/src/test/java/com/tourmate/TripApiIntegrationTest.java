package com.tourmate;

import com.fasterxml.jackson.databind.*;
import com.tourmate.repository.*;
import com.tourmate.service.*;
import com.tourmate.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.jwt-secret=trip-api-test-secret-at-least-32-bytes-long",
 "spring.datasource.url=jdbc:h2:mem:tripapi;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
 "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type=TIMESTAMP", "logging.level.root=ERROR", "logging.level.org.springframework=ERROR", "logging.level.org.hibernate=ERROR"})
@AutoConfigureMockMvc
class TripApiIntegrationTest {
 @MockitoBean AiScheduleAdvisor aiAdvisor;
 @MockitoBean GeoRoutingService routing;
 @MockitoBean PlanLocationService locations;
 @BeforeEach void routeFixtures(){
  when(locations.origin(any())).thenReturn(new RoutePoint(6.9,80.0,"Starting point"));
  when(locations.resolve(any())).thenAnswer(i->{ManualPlanRequest.Stop s=i.getArgument(0);return new ManualPlanRequest.Stop(s.key(),s.title(),s.category(),s.placeSource(),s.placeId(),s.address(),6.8,81.0,s.durationMinutes(),s.preferredTime(),s.estimatedCost(),s.notes(),s.mealType());});
  when(routing.route(any(),any(),anyString())).thenAnswer(i->new RouteEstimate(10,1000,java.util.List.of(i.getArgument(0),i.getArgument(1)),java.util.List.of(0.0,600.0)));
 }
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired TripItemRepository items;
 @Autowired TripDayRepository days;
 private String account() throws Exception {
  String email="trip"+UUID.randomUUID()+"@example.com";
  String response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("firstName","Test","lastName","Traveler","email",email,"password","correct-password"))))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  return json.readTree(response).path("accessToken").asText();
 }
 private JsonNode create(String token) throws Exception {
  return json.readTree(mvc.perform(post("/api/trips").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("""
   {"name":"Ella escape","startDate":"2027-01-01","endDate":"2027-01-03","budget":10000,"travelers":2,"destinationIds":[1],"status":"DRAFT"}
   """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
 }
 private String activity(long day,String start,String end) throws Exception {
  return json.writeValueAsString(Map.of("dayId",day,"title","Walk","category","ACTIVITY","placeSource","CUSTOM","startTime",start,"endTime",end,"estimatedCost",1250.50,"notes","Bring water"));
 }
 @Test void savesAndReloadsAcrossRequestsAndEnforcesOwnership() throws Exception {
  String owner=account(),other=account();JsonNode trip=create(owner);long id=trip.path("id").asLong(),day=trip.path("days").get(0).path("id").asLong();
  mvc.perform(post("/api/trips/"+id+"/items").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(activity(day,"09:00","10:00")))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.estimatedTotal").value(1250.50));
  mvc.perform(get("/api/trips/"+id).header("Authorization","Bearer "+owner))
   .andExpect(status().isOk()).andExpect(jsonPath("$.days[0].items[0].notes").value("Bring water")).andExpect(jsonPath("$.remainingBudget").value(8749.50));
  mvc.perform(get("/api/trips/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/trips/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(get("/api/trips/"+id)).andExpect(status().isUnauthorized());
 }
 @Test void failedChangesRollBackWithoutCorruptingTheSavedSchedule() throws Exception {
  String owner=account();JsonNode trip=create(owner);long id=trip.path("id").asLong(),day=trip.path("days").get(0).path("id").asLong();
  mvc.perform(post("/api/trips/"+id+"/items").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(activity(day,"09:00","10:00"))).andExpect(status().isCreated());
  mvc.perform(post("/api/trips/"+id+"/items").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(activity(day,"09:30","10:30"))).andExpect(status().isConflict());
  String invalid=activity(day,"10:00","11:00").replace("1250.5","-1");
  mvc.perform(post("/api/trips/"+id+"/items").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/trips/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andExpect(jsonPath("$.days[0].items.length()").value(1));
 }
 @Test void committedTripDeletionCascadesAndPrivateListsDoNotLeak() throws Exception {
  String owner=account(),other=account();JsonNode trip=create(owner);long id=trip.path("id").asLong(),day=trip.path("days").get(0).path("id").asLong();
  JsonNode saved=json.readTree(mvc.perform(post("/api/trips/"+id+"/items").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(activity(day,"09:00","10:00")))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  long item=saved.path("days").get(0).path("items").get(0).path("id").asLong();
  mvc.perform(get("/api/trips").header("Authorization","Bearer "+other)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(delete("/api/trips/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk());
  assertFalse(items.existsById(item));assertFalse(days.existsById(day));
  mvc.perform(get("/api/trips/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isNotFound());
 }
 private String guidedInput() {return """
  {"trip":{"name":"Guided Ella trip","startDate":"2027-01-01","endDate":"2027-01-01","budget":10000,"travelers":2,"destinationIds":[1],"status":"DRAFT"},
   "startingLocation":"Colombo hotel","transportMode":"CAR","transferMinutes":30,
   "days":[{"date":"2027-01-01","area":"Ella","startTime":"08:00","endTime":"18:00","stops":[
    {"key":"bridge","title":"Bridge","category":"ATTRACTION","placeSource":"CUSTOM","durationMinutes":90,"estimatedCost":0,"notes":"Bring my camera","mealType":"NONE"},
    {"key":"lunch","title":"Lunch cafe","category":"RESTAURANT","placeSource":"CUSTOM","durationMinutes":60,"preferredTime":"12:30","estimatedCost":1500,"notes":"Vegetarian meal","mealType":"LUNCH"}
   ]}]}
  """;}
 @Test void guidedPlanningPreviewsAndAtomicallySavesTheSelectedVisitsAndMeals() throws Exception {
  String token=account();
  mvc.perform(post("/api/trips/plan/preview").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(guidedInput()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.unscheduled.length()").value(0))
   .andExpect(jsonPath("$.days[0].visits[1].startTime").value("12:30:00"));
  JsonNode saved=json.readTree(mvc.perform(post("/api/trips/plan").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(guidedInput()))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.days[0].items.length()").value(4))
   .andExpect(jsonPath("$.days[0].items[1].notes").value("Bring my camera"))
   .andExpect(jsonPath("$.estimatedTotal").value(1500)).andReturn().getResponse().getContentAsString());
  JsonNode preferences=json.readTree(saved.path("planningPreferences").asText());assertEquals("Colombo hotel",preferences.path("startingLocation").asText());
  mvc.perform(get("/api/trips/"+saved.path("id").asLong()).header("Authorization","Bearer "+token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.days[0].items[3].title").value("Lunch cafe")).andExpect(jsonPath("$.days[0].items[3].mealType").value("LUNCH"));
 }
 @Test void guidedSaveRejectsOverfullPlansAndRollsBackInvalidSelectedPlaces() throws Exception {
  String token=account();String overfull=guidedInput().replace("\"durationMinutes\":90","\"durationMinutes\":720");
  mvc.perform(post("/api/trips/plan").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(overfull)).andExpect(status().isConflict());
  String invalid=guidedInput().replace("\"key\":\"lunch\"", "\"placeId\":\"999999\",\"key\":\"lunch\"");
  int location=invalid.lastIndexOf("\"placeSource\":\"CUSTOM\"");
  invalid=invalid.substring(0,location)+invalid.substring(location).replaceFirst("CUSTOM","TOURMATE");
  mvc.perform(post("/api/trips/plan").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/trips").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
 }
 @Test void aiDraftIsPrivateAndSavesThroughSharedPersistenceWithAiMetadata() throws Exception {
  String owner=account(),other=account();var plan=json.readTree(guidedInput());((com.fasterxml.jackson.databind.node.ArrayNode)plan.path("days").get(0).path("stops")).remove(1);
  when(aiAdvisor.model()).thenReturn("test-model");when(aiAdvisor.advise(any())).thenReturn(new AiScheduleAdvisor.Advice(List.of(new AiScheduleAdvisor.Choice("bridge",180,"Allow an immersive visit, with time for photographs.")),List.of("Check opening hours.")));
  String body=json.writeValueAsString(Map.of("plan",plan,"preferences","Photography and a relaxed pace","pace","RELAXED","meals",List.of()));
  mvc.perform(post("/api/trips/ai/preview").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
  var result=json.readTree(mvc.perform(post("/api/trips/ai/preview").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.plan.days[0].stops[0].durationMinutes").value(180)).andReturn().getResponse().getContentAsString());
  String endpoint="/api/trips/ai/"+result.path("draftId").asText()+"/save";
  mvc.perform(post(endpoint).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  var saved=json.readTree(mvc.perform(post(endpoint).header("Authorization","Bearer "+owner)).andExpect(status().isCreated()).andExpect(jsonPath("$.planningMode").value("AI")).andReturn().getResponse().getContentAsString());
  assertEquals("test-model",json.readTree(saved.path("planningPreferences").asText()).path("ai").path("model").asText());assertEquals(2,saved.path("days").get(0).path("items").size());
  mvc.perform(post(endpoint).header("Authorization","Bearer "+owner)).andExpect(status().isNotFound());
  mvc.perform(get("/api/trips/"+saved.path("id").asLong()).header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andExpect(jsonPath("$.planningMode").value("AI"));
 }

}
