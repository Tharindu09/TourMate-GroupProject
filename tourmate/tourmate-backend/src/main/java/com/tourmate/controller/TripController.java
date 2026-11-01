package com.tourmate.controller;
import com.tourmate.dto.*;
import com.tourmate.service.TripService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/trips")
public class TripController {
 private final TripService trips;
 public TripController(TripService trips) {this.trips=trips;}
 private Long user(Jwt jwt) {return Long.valueOf(jwt.getSubject());}
 @GetMapping public PageResponse<TripSummary> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size) {return trips.list(user(jwt),page,size);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public TripResponse create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TripRequest body) {return trips.create(user(jwt),body);}
 @GetMapping("/{id}") public TripResponse detail(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {return trips.detail(user(jwt),id);}
 @PutMapping("/{id}") public TripResponse update(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody TripRequest body) {return trips.update(user(jwt),id,body);}
 @DeleteMapping("/{id}") public Map<String,Boolean> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) {trips.delete(user(jwt),id);return Map.of("deleted",true);}
 @PostMapping("/{id}/items") @ResponseStatus(HttpStatus.CREATED)
 public TripResponse add(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody TripItemRequest body) {return trips.addItem(user(jwt),id,body);}
 @PutMapping("/{id}/items/{itemId}") public TripResponse updateItem(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long itemId,@Valid @RequestBody TripItemRequest body) {return trips.updateItem(user(jwt),id,itemId,body);}
 @DeleteMapping("/{id}/items/{itemId}") public TripResponse deleteItem(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long itemId) {return trips.deleteItem(user(jwt),id,itemId);}
 @PutMapping("/{id}/days/{dayId}/order") public TripResponse reorder(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long dayId,@Valid @RequestBody TripOrderRequest body) {return trips.reorder(user(jwt),id,dayId,body.itemIds());}
 @PutMapping("/{id}/items/{itemId}/move") public TripResponse move(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long itemId,@Valid @RequestBody TripMoveRequest body) {return trips.move(user(jwt),id,itemId,body.dayId(),body.position());}
}
