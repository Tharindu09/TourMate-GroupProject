package com.tourmate.dto;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
    public static Pageable pageable(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size between 1 and 100");
        return PageRequest.of(page, size, Sort.by(sort).ascending().and(Sort.by("id")));
    }
}
