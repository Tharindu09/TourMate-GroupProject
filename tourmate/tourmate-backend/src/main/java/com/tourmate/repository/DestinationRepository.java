package com.tourmate.repository;

import com.tourmate.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    long countByIdIn(java.util.Collection<Long> ids);
    @Query("select d from Destination d where lower(concat(d.name, ' ', d.city, ' ', d.country)) like lower(concat('%', :q, '%'))")
    Page<Destination> search(String q, Pageable pageable);
}
