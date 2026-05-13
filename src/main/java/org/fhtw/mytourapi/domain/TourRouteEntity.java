package org.fhtw.mytourapi.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tour_routes")
public class TourRouteEntity {

    @Id
    @Column(name = "tour_id")
    private Long tourId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private TourEntity tour;

    @Column(name = "route_source", nullable = false, length = 50)
    private String routeSource = "OPENROUTESERVICE";

    @Column(name = "route_profile", nullable = false, length = 50)
    private String routeProfile;

    @Column(name = "start_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal startLat;

    @Column(name = "start_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal startLon;

    @Column(name = "end_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal endLat;

    @Column(name = "end_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal endLon;

    @Column(name = "midpoint_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal midpointLat;

    @Column(name = "midpoint_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal midpointLon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_geometry", nullable = false, columnDefinition = "jsonb")
    private JsonNode routeGeometry;

    @CreationTimestamp
    @Column(name = "route_fetched_at", nullable = false, updatable = false)
    private Instant routeFetchedAt;
}
