package org.fhtw.mytourapi.repository;

import org.fhtw.mytourapi.domain.ChildFriendlinessCategory;
import org.fhtw.mytourapi.domain.PopularityCategory;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.TransportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<TourEntity, Long> {

    List<TourEntity> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<TourEntity> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);

    List<TourEntity> findAllByUser_IdAndTransportTypeOrderByUpdatedAtDesc(Long userId, TransportType transportType);

    List<TourEntity> findAllByUser_IdAndPopularityCategoryOrderByUpdatedAtDesc(
            Long userId,
            PopularityCategory popularityCategory
    );

    List<TourEntity> findAllByUser_IdAndChildFriendlinessCategoryOrderByUpdatedAtDesc(
            Long userId,
            ChildFriendlinessCategory childFriendlinessCategory
    );
}
