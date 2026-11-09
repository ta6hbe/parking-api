package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.helper.AdjustableClock;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Supplies acceptance dependencies and fresh shared state for each scenario.
 */
@TestConfiguration(proxyBeanMethods = false)
public class AcceptanceTestConfiguration {

    @Bean
    @ScenarioScope
    ScenarioContext scenarioContext() {
        return new ScenarioContext();
    }

    @Bean
    RestClient restClient(Environment environment) {
        int port = environment.getRequiredProperty("server.port", Integer.class);
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("http://localhost:" + port)
                .requestFactory(requestFactory).build();
    }

    @Bean
    @Primary
    AdjustableClock adjustableClock() {
        return new AdjustableClock("2026-11-08T00:00:00Z");
    }

    @Bean
    AtHelper atHelper(
            RestClient restClient,
            AdjustableClock clock,
            Environment environment) {
        return new AtHelper(restClient, clock,
                environment.getRequiredProperty("parking.default-location-id", Long.class));
    }
}
