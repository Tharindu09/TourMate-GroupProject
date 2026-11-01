package com.tourmate.controller;

import com.tourmate.dto.GeoPlace;
import com.tourmate.dto.GeoSearchResult;
import com.tourmate.dto.PlacePhoto;
import com.tourmate.service.GeoapifyService;
import com.tourmate.service.PlacePhotoService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/geoapify")
public class GeoapifyController {
    private final GeoapifyService places;
    private final PlacePhotoService photos;
    public GeoapifyController(GeoapifyService places, PlacePhotoService photos) {this.places = places; this.photos = photos;}
    @GetMapping("/search")
    public ResponseEntity<GeoSearchResult> search(@RequestParam String q, @RequestParam(defaultValue="") String type,
        @RequestParam(defaultValue="") String name, @RequestParam(defaultValue="10000") int radius,
        @RequestParam(defaultValue="0") int page, @RequestParam(required=false) Double lat, @RequestParam(required=false) Double lon) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(places.search(q, type, name, radius, page, lat, lon));
    }
    @GetMapping("/photos/{id}")
    public ResponseEntity<PlacePhoto> photo(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(30))).body(photos.photo(id));
    }
    @GetMapping("/details/{id}")
    public ResponseEntity<GeoPlace> detail(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(places.detail(id));
    }
}
