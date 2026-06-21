package org.fhtw.mytourapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.openmeteo")
public class OpenMeteoProperties {

    private boolean enabled = true;
    private String archiveBaseUrl = "https://archive-api.open-meteo.com";
    private String forecastBaseUrl = "https://api.open-meteo.com";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    private int forecastPastDays = 92;
    private int forecastDays = 16;

    public boolean shouldUseApi() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getArchiveBaseUrl() {
        return archiveBaseUrl;
    }

    public void setArchiveBaseUrl(String archiveBaseUrl) {
        this.archiveBaseUrl = archiveBaseUrl;
    }

    public String getForecastBaseUrl() {
        return forecastBaseUrl;
    }

    public void setForecastBaseUrl(String forecastBaseUrl) {
        this.forecastBaseUrl = forecastBaseUrl;
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

    public int getForecastPastDays() {
        return forecastPastDays;
    }

    public void setForecastPastDays(int forecastPastDays) {
        this.forecastPastDays = forecastPastDays;
    }

    public int getForecastDays() {
        return forecastDays;
    }

    public void setForecastDays(int forecastDays) {
        this.forecastDays = forecastDays;
    }
}
