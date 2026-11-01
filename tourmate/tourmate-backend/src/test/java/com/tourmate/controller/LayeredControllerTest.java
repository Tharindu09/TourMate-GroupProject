package com.tourmate.controller;

import com.tourmate.config.SecurityConfig;
import com.tourmate.dto.*;
import com.tourmate.model.Destination;
import com.tourmate.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, UserController.class, DestinationController.class, PlaceController.class},
    properties = "app.jwt-secret=controller-test-secret-at-least-32-bytes-long")
@Import(SecurityConfig.class)
class LayeredControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;
    @MockitoBean UserService users;
    @MockitoBean DestinationService destinations;
    @MockitoBean PlaceService places;

    @Test void registrationKeepsItsStatusAndResponseAndDelegatesToService() throws Exception {
        when(auth.register(any())).thenReturn(new AuthResponse("signed-token", "Bearer", 3600,
            new UserProfile(7L, "Test", "Traveller", "test@example.com", null, "USER")));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"firstName":"Test","lastName":"Traveller","email":"test@example.com","password":"correct-password"}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").value("signed-token"))
            .andExpect(jsonPath("$.user.id").value(7)).andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        verify(auth).register(new RegisterRequest("Test", "Traveller", "test@example.com", "correct-password"));
    }
    @Test void invalidRegistrationStopsAtTheControllerBoundary() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"firstName":"","lastName":"Traveller","email":"invalid","password":"short"}
            """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
    }
    @Test void protectedProfileStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }
    @Test void profileUpdatePassesTheAuthenticatedIdentityToService() throws Exception {
        when(users.update(eq(7L), any())).thenReturn(new UserProfile(7L, "Updated", "Traveller", "test@example.com", null, "USER"));
        mvc.perform(put("/api/users/me").with(jwt().jwt(token -> token.subject("7")))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Updated","lastName":"Traveller","role":"ADMIN"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        verify(users).update(7L, new UpdateProfileRequest("Updated", "Traveller"));
    }
    @Test void publicDestinationSearchKeepsItsQueryParametersAndEnvelope() throws Exception {
        when(destinations.search("Ella", 1, 5)).thenReturn(new PageResponse<Destination>(List.of(), 0, 0, 1, 5));
        mvc.perform(get("/api/destinations/search").param("q", "Ella").param("page", "1").param("size", "5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.number").value(1));
        verify(destinations).search("Ella", 1, 5);
    }
}
