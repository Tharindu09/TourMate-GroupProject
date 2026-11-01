package com.tourmate.controller;
import com.tourmate.dto.*;
import com.tourmate.service.AiTripPlanService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/trips/ai")
public class AiTripPlanController {
 private final AiTripPlanService plans;
 public AiTripPlanController(AiTripPlanService plans){this.plans=plans;}
 @PostMapping("/preview") public AiPlanResponse preview(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AiPlanRequest request){return plans.generate(Long.valueOf(jwt.getSubject()),request);}
 @PostMapping("/{draftId}/save") @ResponseStatus(HttpStatus.CREATED) public TripResponse save(@AuthenticationPrincipal Jwt jwt,@PathVariable String draftId){return plans.save(Long.valueOf(jwt.getSubject()),draftId);}
}
