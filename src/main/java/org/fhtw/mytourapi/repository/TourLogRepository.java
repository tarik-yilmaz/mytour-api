package org.fhtw.mytourapi.repository;

import org.fhtw.mytourapi.domain.TourLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourLogRepository extends JpaRepository<TourLogEntity, Long> {

    List<TourLogEntity> findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc(Long tourId, Long userId);

    Optional<TourLogEntity> findByIdAndTour_IdAndTour_User_Id(Long id, Long tourId, Long userId);

    boolean existsByIdAndTour_IdAndTour_User_Id(Long id, Long tourId, Long userId);

    long countByTour_IdAndTour_User_Id(Long tourId, Long userId);
}
