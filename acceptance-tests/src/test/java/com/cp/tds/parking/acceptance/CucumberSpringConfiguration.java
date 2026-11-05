package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.api.ParkingApiApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the acceptance application on its configured port and renews the context per scenario.
 */
@CucumberContextConfiguration
@SpringBootTest(
        classes = ParkingApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@Import(AcceptanceTestConfiguration.class)
@ActiveProfiles("acceptance")
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
public class CucumberSpringConfiguration {
}
