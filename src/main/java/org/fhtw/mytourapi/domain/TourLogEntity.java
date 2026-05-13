package org.fhtw.mytourapi.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tour_logs")
public class TourLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourEntity tour;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "difficulty", nullable = false)
    private Short difficulty;

    @Column(name = "total_distance_m", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDistanceM;

    @Column(name = "total_time_s", nullable = false)
    private Integer totalTimeS;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(mappedBy = "tourLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TourLogWeatherEntity weather;
}
