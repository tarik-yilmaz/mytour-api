package org.fhtw.mytourapi.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tours")
public class TourEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "start_location", nullable = false)
    private String startLocation;

    @Column(name = "end_location", nullable = false)
    private String endLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 20)
    private TransportType transportType;

    @Column(name = "timezone_id", nullable = false, length = 64)
    private String timezoneId = "UTC";

    @Column(name = "planned_distance_m", nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedDistanceM = BigDecimal.ZERO;

    @Column(name = "estimated_duration_s", nullable = false)
    private Integer estimatedDurationS = 0;

    @Column(name = "cover_image_path", length = 1024)
    private String coverImagePath;

    @Column(name = "cover_image_original_filename")
    private String coverImageOriginalFilename;

    @Column(name = "cover_image_content_type", length = 100)
    private String coverImageContentType;

    @Column(name = "cover_image_size_bytes")
    private Long coverImageSizeBytes;

    @Column(name = "log_count", nullable = false)
    private Integer logCount = 0;

    @Column(name = "popularity_score", nullable = false)
    private Integer popularityScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "popularity_category", nullable = false, length = 40)
    private PopularityCategory popularityCategory = PopularityCategory.NEW;

    @Column(name = "popularity_label", nullable = false, length = 80)
    private String popularityLabel = "new";

    @Column(name = "child_friendliness_score", nullable = false)
    private Integer childFriendlinessScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "child_friendliness_category", nullable = false, length = 40)
    private ChildFriendlinessCategory childFriendlinessCategory = ChildFriendlinessCategory.UNKNOWN;

    @Column(name = "child_friendliness_label", nullable = false, length = 80)
    private String childFriendlinessLabel = "unknown";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TourRouteEntity route;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TourLogEntity> logs = new ArrayList<>();
}
