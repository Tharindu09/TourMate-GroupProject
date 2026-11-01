package com.tourmate.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record AiPlanRequest(@NotNull @Valid ManualPlanRequest plan,
 @NotBlank @Size(max=2000) String preferences,
 @NotNull @Pattern(regexp="RELAXED|BALANCED|ACTIVE") String pace,
 @NotNull @Size(max=3) List<@Pattern(regexp="BREAKFAST|LUNCH|DINNER") String> meals, boolean includeHotels) {
 public AiPlanRequest(ManualPlanRequest plan,String preferences,String pace,List<String> meals){this(plan,preferences,pace,meals,false);}
}
