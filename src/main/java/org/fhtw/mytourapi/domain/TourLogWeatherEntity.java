package org.fhtw.mytourapi.domain;

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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tour_log_weather")
public class TourLogWeatherEntity {

    @Id
    @Column(name = "tour_log_id")
    private Long tourLogId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_log_id", nullable = false)
    private TourLogEntity tourLog;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider = "OPEN_METEO";

    @Column(name = "provider_dataset", length = 100)
    private String providerDataset;

    @Column(name = "lookup_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lookupLat;

    @Column(name = "lookup_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal lookupLon;

    @Column(name = "weather_observed_at", nullable = false)
    private Instant weatherObservedAt;

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "relative_humidity_percent", precision = 5, scale = 2)
    private BigDecimal relativeHumidityPercent;

    @Column(name = "precipitation_mm", precision = 8, scale = 2)
    private BigDecimal precipitationMm;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "weather_description", length = 120)
    private String weatherDescription;

    @Column(name = "wind_speed_kmh", precision = 6, scale = 2)
    private BigDecimal windSpeedKmh;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
