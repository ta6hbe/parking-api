package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.config.ParkingProperties;
import com.cp.tds.parking.api.persistence.ActiveParkingStayRepositoryI;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.time.Clock;

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
        return RestClient.create("http://localhost:" + port);
    }

    @Bean
    AtHelper atHelper(
            ParkingProperties parkingProperties,
            RestClient restClient,
            ActiveParkingStayRepositoryI activeStaysRepository,
            Clock clock) {
        return new AtHelper(parkingProperties, restClient, activeStaysRepository, clock);
    }
}
