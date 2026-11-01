package com.tourmate.controller;

import com.tourmate.dto.MapLocation;
import com.tourmate.dto.TravelPlace;
import com.tourmate.dto.TravelSearchResult;
import com.tourmate.service.RapidTravelService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rapidapi")
public class RapidTravelController {
    private final RapidTravelService travel;
    public RapidTravelController(RapidTravelService travel) {this.travel = travel;}
    @GetMapping("/search")
    public ResponseEntity<TravelSearchResult> search(@RequestParam String q, @RequestParam(defaultValue="ATTRACTION") String type,
        @RequestParam(defaultValue="10000") int radius, @RequestParam(defaultValue="0") int page,
        @RequestParam(required=false) Double lat, @RequestParam(required=false) Double lon,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkin,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkout,
        @RequestParam(defaultValue="1") int rooms, @RequestParam(defaultValue="2") int adults) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(travel.search(q, type, radius, page, lat, lon, checkin, checkout, rooms, adults));
    }
    @GetMapping("/map/{type}/{id}")
    public ResponseEntity<MapLocation> mapLocation(@PathVariable String type, @PathVariable String id, @RequestParam String resultToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(travel.mapLocation(type, id, resultToken));
    }
    @GetMapping("/details/{type}/{id}")
    public ResponseEntity<TravelPlace> detail(@PathVariable String type, @PathVariable String id, @RequestParam(required=false) String resultToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(travel.detail(type, id, resultToken));
    }
}
