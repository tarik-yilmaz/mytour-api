package org.fhtw.mytourapi.config;

import org.fhtw.mytourapi.dto.TransportType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.openrouteservice")
public class OpenRouteServiceProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.openrouteservice.org";
    private String apiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    private String bikeProfile = "cycling-regular";
    private String hikeProfile = "foot-hiking";
    private String runningProfile = "foot-walking";
    private String vacationProfile = "driving-car";
    private String geocodeBoundaryCountry = "AT";

    public boolean shouldUseApi() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String profileFor(TransportType transportType) {
        return switch (transportType) {
            case BIKE -> bikeProfile;
            case HIKE -> hikeProfile;
            case RUNNING -> runningProfile;
            case VACATION -> vacationProfile;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getBikeProfile() {
        return bikeProfile;
    }

    public void setBikeProfile(String bikeProfile) {
        this.bikeProfile = bikeProfile;
    }

    public String getHikeProfile() {
        return hikeProfile;
    }

    public void setHikeProfile(String hikeProfile) {
        this.hikeProfile = hikeProfile;
    }

    public String getRunningProfile() {
        return runningProfile;
    }

    public void setRunningProfile(String runningProfile) {
        this.runningProfile = runningProfile;
    }

    public String getVacationProfile() {
        return vacationProfile;
    }

    public void setVacationProfile(String vacationProfile) {
        this.vacationProfile = vacationProfile;
    }

    public String getGeocodeBoundaryCountry() {
        return geocodeBoundaryCountry;
    }

    public void setGeocodeBoundaryCountry(String geocodeBoundaryCountry) {
        this.geocodeBoundaryCountry = geocodeBoundaryCountry;
    }
}
