package com.tourmate.service;

import com.tourmate.dto.PageResponse;
import com.tourmate.model.Destination;
import com.tourmate.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class DestinationService {
    private final DestinationRepository destinations;
    public DestinationService(DestinationRepository destinations) {this.destinations = destinations;}
    public PageResponse<Destination> search(String q, int page, int size) {
        return PageResponse.from(destinations.search(q.strip(), PageResponse.pageable(page, size, "name")));
    }
    public Destination detail(Long id) {
        return destinations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
    }
}
