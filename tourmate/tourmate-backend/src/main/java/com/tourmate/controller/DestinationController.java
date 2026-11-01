package com.tourmate.controller;

import com.tourmate.dto.PageResponse;
import com.tourmate.model.Destination;
import com.tourmate.service.DestinationService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationService destinations;
    public DestinationController(DestinationService destinations) { this.destinations = destinations; }
    @GetMapping({"", "/search"})
    public PageResponse<Destination> list(@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        return destinations.search(q, page, size);
    }
    @GetMapping("/{id}") public Destination detail(@PathVariable Long id) {
        return destinations.detail(id);
    }
}
