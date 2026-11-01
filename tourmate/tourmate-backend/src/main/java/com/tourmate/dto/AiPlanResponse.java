package com.tourmate.dto;
import java.util.List;
public record AiPlanResponse(String draftId,ManualPlanRequest plan,ManualPlanPreview preview,List<String> explanations,List<String> warnings,String model) {}
