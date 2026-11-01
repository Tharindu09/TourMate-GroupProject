package com.tourmate.repository;
import com.tourmate.model.TripDay;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TripDayRepository extends JpaRepository<TripDay,Long> {
 List<TripDay> findByTripIdOrderByDateAsc(Long tripId);
 Optional<TripDay> findByIdAndTripId(Long id, Long tripId);
}
