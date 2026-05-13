package org.fhtw.mytourapi.repository;

import org.fhtw.mytourapi.domain.TourRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TourRouteRepository extends JpaRepository<TourRouteEntity, Long> {

    Optional<TourRouteEntity> findByTour_IdAndTour_User_Id(Long tourId, Long userId);
}
