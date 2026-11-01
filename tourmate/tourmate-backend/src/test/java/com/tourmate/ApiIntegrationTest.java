package com.tourmate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest(properties="app.jwt-secret=integration-test-secret-at-least-32-bytes-long")
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {
    @Container @ServiceConnection static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void registrationLoginAndProtectedProfile() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        String registration = """
            {"firstName":"Test","lastName":"Traveller","email":"Traveller@example.com","password":"correct-password"}
            """;
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.user.email").value("traveller@example.com"))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Test"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"traveller@example.com\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"traveller@example.com\",\"password\":\"correct-password\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        mvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Updated\",\"lastName\":\"Traveller\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token + "tampered")).andExpect(status().isUnauthorized());
    }
    @Test void destinationsAndCombinedFilters() throws Exception {
        mvc.perform(get("/api/destinations/search").param("q", "ella")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Ella"));
        mvc.perform(get("/api/places").param("destinationId", "1").param("type", "HOTEL").param("maxPrice", "15000"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/places").param("destinationId", "1").param("type", "HOTEL").param("maxPrice", "10000"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/places").param("minRating", "4")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/places").param("q", "%")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/places").param("minRating", "6")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/places").param("size", "1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/places/99999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email", notNullValue()));
    }
}
