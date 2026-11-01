package com.tourmate.repository;
import com.tourmate.model.Trip;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface TripRepository extends JpaRepository<Trip,Long> {
 Page<Trip> findByUserIdOrderByUpdatedAtDescIdDesc(Long userId, Pageable pageable);
 Optional<Trip> findByIdAndUserId(Long id, Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select t from Trip t where t.id = :id and t.userId = :userId")
 Optional<Trip> lockOwned(Long id, Long userId);
}
