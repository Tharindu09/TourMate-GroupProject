package com.tourmate.service;
import com.tourmate.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ManualTripPlanService {
 private final ManualSchedulePlanner planner;private final TripPlanWriter writer;
 public ManualTripPlanService(ManualSchedulePlanner planner,TripPlanWriter writer){this.planner=planner;this.writer=writer;}
 public ManualPlanPreview preview(ManualPlanRequest input){return planner.preview(input);}
 public TripResponse save(Long userId,ManualPlanRequest input){
  // External routing is completed before the persistence transaction begins.
  var preview=planner.preview(input);if(!preview.unscheduled().isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Some selected stops are unreachable in the chosen times. Adjust the route and preview again.");
  return writer.save(userId,input,preview);
 }
}
