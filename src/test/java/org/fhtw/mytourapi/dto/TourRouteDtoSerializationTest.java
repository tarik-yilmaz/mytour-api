package org.fhtw.mytourapi.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TourRouteDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void routeGeometrySerializesAsGeoJsonWithFeatures() throws Exception {
        TourRouteDto route = new TourRouteDto(
                "OPENROUTESERVICE",
                "cycling-regular",
                coordinate("48.2082", "16.3738"),
                coordinate("48.2500", "16.4000"),
                coordinate("48.2291", "16.3869"),
                geoJson(),
                null
        );

        String json = objectMapper.writeValueAsString(route);

        assertThat(json).contains("\"routeGeometry\"");
        assertThat(json).contains("\"type\":\"FeatureCollection\"");
        assertThat(json).contains("\"features\"");
        assertThat(json).contains("\"type\":\"Feature\"");
        assertThat(json).contains("\"geometry\"");
        assertThat(json).contains("\"coordinates\"");
        assertThat(json).doesNotContain("\"nodeType\"");
        assertThat(json).doesNotContain("\"array\"");
        assertThat(json).doesNotContain("\"containerNode\"");
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private static Map<String, Object> geoJson() {
        return Map.of(
                "type", "FeatureCollection",
                "features", List.of(Map.of(
                        "type", "Feature",
                        "geometry", Map.of(
                                "type", "LineString",
                                "coordinates", List.of(
                                        List.of(new BigDecimal("16.3738"), new BigDecimal("48.2082")),
                                        List.of(new BigDecimal("16.4000"), new BigDecimal("48.2500"))
                                )
                        )
                ))
        );
    }
}
