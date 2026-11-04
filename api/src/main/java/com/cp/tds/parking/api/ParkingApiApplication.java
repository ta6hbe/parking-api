package com.cp.tds.parking.api;

import com.cp.tds.parking.api.domain.ParkingStay;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts the parking API and provides the shared UTC clock.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ParkingApiApplication {

	@Bean
	Clock parkingClock() {
		return Clock.systemUTC();
	}

	public static void main(String[] args) {
		SpringApplication.run(ParkingApiApplication.class, args);
	}

}
