package com.tourmate.place;
import com.tourmate.common.PageResponse;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/places")
public class PlaceController {
    private final PlaceService places;
    public PlaceController(PlaceService places) { this.places = places; }
    @GetMapping public PageResponse<Place> list(@RequestParam(required=false) Long destinationId,
        @RequestParam(required=false) PlaceType type, @RequestParam(required=false) BigDecimal minRating,
        @RequestParam(required=false) BigDecimal maxPrice, @RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        return places.search(destinationId, type, minRating, maxPrice, q, page, size);
    }
    @GetMapping("/{id}") public Place detail(@PathVariable Long id) { return places.detail(id); }
}
