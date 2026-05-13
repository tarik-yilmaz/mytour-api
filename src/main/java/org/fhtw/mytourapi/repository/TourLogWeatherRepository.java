package org.fhtw.mytourapi.repository;

import org.fhtw.mytourapi.domain.TourLogWeatherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TourLogWeatherRepository extends JpaRepository<TourLogWeatherEntity, Long> {

    Optional<TourLogWeatherEntity> findByTourLog_IdAndTourLog_Tour_IdAndTourLog_Tour_User_Id(
            Long tourLogId,
            Long tourId,
            Long userId
    );
}
