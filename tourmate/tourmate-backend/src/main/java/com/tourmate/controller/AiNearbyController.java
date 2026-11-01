package com.tourmate.controller;
import com.tourmate.dto.PlanNearbyRequest;
import com.tourmate.service.AiNearbyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/trips/ai")
public class AiNearbyController {
 private final AiNearbyService service;
 public AiNearbyController(AiNearbyService service){this.service=service;}
 public record Request(@NotNull @Valid PlanNearbyRequest nearby,@NotBlank @Size(max=2000) String preferences) {}
 @PostMapping("/nearby") public AiNearbyService.Ranked nearby(@Valid @RequestBody Request request){return service.recommend(request.nearby(),request.preferences());}
}
