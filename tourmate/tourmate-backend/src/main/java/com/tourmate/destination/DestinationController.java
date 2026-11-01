package com.tourmate.destination;
import com.tourmate.common.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationRepository destinations;
    public DestinationController(DestinationRepository destinations) { this.destinations = destinations; }
    @GetMapping({"", "/search"})
    public PageResponse<Destination> list(@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        return PageResponse.from(destinations.search(q.strip(), PageResponse.pageable(page, size, "name")));
    }
    @GetMapping("/{id}") public Destination detail(@PathVariable Long id) {
        return destinations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }
}
