package com.tourmate.place;
import org.springframework.data.jpa.repository.*;
public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {}
