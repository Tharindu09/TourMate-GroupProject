package com.tourmate.destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    @Query("select d from Destination d where lower(concat(d.name, ' ', d.city, ' ', d.country)) like lower(concat('%', :q, '%'))")
    Page<Destination> search(String q, Pageable pageable);
}
