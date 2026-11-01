package com.tourmate.repository;
import com.tourmate.model.TripItem;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TripItemRepository extends JpaRepository<TripItem,Long> {
 List<TripItem> findByDayIdOrderByPositionAscIdAsc(Long dayId);
 List<TripItem> findByDayIdInOrderByPositionAscIdAsc(Collection<Long> dayIds);
}
