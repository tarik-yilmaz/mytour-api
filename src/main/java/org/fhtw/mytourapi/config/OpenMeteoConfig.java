package org.fhtw.mytourapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenMeteoProperties.class)
public class OpenMeteoConfig {

    @Bean
    public RestClient openMeteoArchiveRestClient(OpenMeteoProperties properties) {
        return restClient(properties.getArchiveBaseUrl(), properties);
    }

    @Bean
    public RestClient openMeteoForecastRestClient(OpenMeteoProperties properties) {
        return restClient(properties.getForecastBaseUrl(), properties);
    }

    private RestClient restClient(String baseUrl, OpenMeteoProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
