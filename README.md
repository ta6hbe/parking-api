# Parking API

A car park management REST API for the Test Driven Solutions Java take-home task.

## Current status

The repository currently contains the Spring Boot application skeleton and an application-context test. Parking endpoints, business rules, the separate acceptance-test module, and Docker packaging are planned but not yet implemented.

## Prerequisites

- Java 21, with `JAVA_HOME` pointing to the JDK.
- Network access on the first build to download Maven and project dependencies.

The project uses Spring Boot 4.1.2. The included Maven Wrapper selects Maven 3.10.0; a separate Maven installation is not required.

Run the following commands from the repository root. On Windows, replace `./mvnw` with `mvnw.cmd`.

## Run locally

```sh
./mvnw spring-boot:run
```

The application uses port 8080 by default. No parking routes are implemented yet, so HTTP 404 responses for those routes are expected at this stage.

To select another port:

```sh
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Run tests

```sh
./mvnw test
```

Currently, this runs the generated application-context test. JUnit tests with AssertJ assertions, selective integration tests, and a separate Cucumber acceptance suite will be added incrementally. The acceptance profile is not yet configured.

## Package and run the executable JAR

```sh
./mvnw clean package
java -jar target/parking-api-0.0.1-SNAPSHOT.jar
```

## Design and assumptions

See the [design proposal](documents/parking-api-design-proposal.md) for requirements, agreed assumptions, architectural decisions, and alternatives considered.

The planned API will allocate parking spaces, report occupancy, and calculate a final charge on departure. State will be held in memory, with bounded billed-session history and no persistent store. Restarting the application will clear that state once implemented.

Location and billing configuration, environment-variable overrides, and basic Docker build/run instructions will be documented as those increments are implemented.
