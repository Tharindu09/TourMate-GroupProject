package com.tourmate.service;

import com.tourmate.dto.PageResponse;
import com.tourmate.model.Place;
import com.tourmate.model.PlaceType;
import com.tourmate.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PlaceService {
    private final PlaceRepository places;
    public PlaceService(PlaceRepository places) { this.places = places; }
    public PageResponse<Place> search(Long destinationId, PlaceType type, BigDecimal minRating, BigDecimal maxPrice, String q, int page, int size) {
        if (minRating != null && (minRating.signum() < 0 || minRating.compareTo(BigDecimal.valueOf(5)) > 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 0 and 5");
        if (maxPrice != null && maxPrice.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be non-negative");
        Specification<Place> spec = (root, query, cb) -> cb.conjunction();
        if (destinationId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("destinationId"), destinationId));
        if (type != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("type"), type));
        if (minRating != null) spec = spec.and((r, query, cb) -> cb.greaterThanOrEqualTo(r.get("averageRating"), minRating));
        if (maxPrice != null) spec = spec.and((r, query, cb) -> cb.lessThanOrEqualTo(r.get("estimatedCost"), maxPrice));
        if (!q.isBlank()) {
            String term = "%" + q.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("name")), term, '\\'));
        }
        return PageResponse.from(places.findAll(spec, PageResponse.pageable(page, size, "name")));
    }
    public Place detail(Long id) {
        return places.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));
    }
}
