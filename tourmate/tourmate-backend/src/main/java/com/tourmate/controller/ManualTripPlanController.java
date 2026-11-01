package com.tourmate.controller;
import com.tourmate.dto.*;
import com.tourmate.service.ManualTripPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/trips/plan")
public class ManualTripPlanController {
 private final ManualTripPlanService plans;
 private final com.tourmate.service.PlanNearbyService nearby;
 public ManualTripPlanController(ManualTripPlanService plans,com.tourmate.service.PlanNearbyService nearby){this.plans=plans;this.nearby=nearby;}
 @PostMapping("/meals") public PlanMealOptionsResponse meals(@Valid @RequestBody PlanMealOptionsRequest body){return nearby.meals(body);}
 @PostMapping("/nearby") public PlanNearbyResponse nearby(@Valid @RequestBody PlanNearbyRequest body){return nearby.options(body);}
 @PostMapping("/preview") public ManualPlanPreview preview(@Valid @RequestBody ManualPlanRequest body){return plans.preview(body);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public TripResponse save(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ManualPlanRequest body){return plans.save(Long.valueOf(jwt.getSubject()),body);}
}
