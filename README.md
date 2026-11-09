# Parking API

A car park management REST API for the Test Driven Solutions Java take-home task. It allocates parking spaces, reports occupancy, calculates final charges, and releases spaces on departure. All state is held in process memory.

The [solution design and closed decisions](documents/parking-api-solution-design.md) describes the current architecture, every implemented workflow, assumptions, rejected alternatives, and deferred work.

## Prerequisites

- Java 21, with `JAVA_HOME` pointing to the JDK when building.
- Network access on the first build to download Maven and dependencies.

The project uses Spring Boot 4.1.2 and the included Maven Wrapper selects Maven 3.10.0. A separate Maven installation is unnecessary. Docker, a database, and a separate application server are not required.

Run commands from the repository root, which contains `pom.xml`, `api`, and `acceptance-tests`. On Windows, replace `./mvnw` with `mvnw.cmd`.

## Build and verify

```sh
./mvnw clean verify
```

This runs API-module tests, packages the application, and runs the HTTP acceptance suite. Acceptance execution uses port **8081**, which must be available. The acceptance profile is a Spring profile configured by the tests; no Maven `-Pacceptance` option is needed.

To run only the API-module tests:

```sh
./mvnw -pl api test
```

To build packages without running the Failsafe acceptance phase:

```sh
./mvnw clean package
```

`package` still runs API tests. Use `verify` for the complete acceptance result.

## Run the application

After building, run the executable fat JAR:

```sh
java -jar api/target/api-0.0.1-SNAPSHOT-exec.jar
```

The `-exec.jar` contains application dependencies, an embedded web server, and default configuration. Once it is built, that single file can be copied elsewhere and run with Java 21 without Maven or the source tree. The regular `api-0.0.1-SNAPSHOT.jar` is retained for module dependencies; use the `-exec.jar` for execution.

The application listens on port **8080** by default. To override it:

```sh
java -jar api/target/api-0.0.1-SNAPSHOT-exec.jar --server.port=9090
```

Alternatively, run directly from the API module through Maven:

```sh
./mvnw -pl api spring-boot:run
```

Stop with Ctrl+C. Restarting clears active stays, temporary reservations, and completed history.

## Try the API

In another terminal, query the default location:

```sh
curl -i http://localhost:8080/parking
```

Park a small vehicle:

```sh
curl -i -X POST http://localhost:8080/parking \
  -H 'Content-Type: application/json' \
  -d '{"vehicleReg":"ab 12 cde","vehicleType":1}'
```

The response is **201 Created**, containing `parkingSessionId`, normalised `vehicleReg`, `spaceNumber`, and `timeIn`.

Depart and retrieve the final bill:

```sh
curl -i -X POST http://localhost:8080/parking/bill \
  -H 'Content-Type: application/json' \
  -d '{"vehicleReg":"AB12CDE"}'
```

The response is **200 OK**, containing the same `parkingSessionId`, `vehicleReg`, `vehicleCharge`, `timeIn`, and `timeOut`. The charge depends on actual elapsed time. Query occupancy again to check that the space has been released. A later departure request without a new arrival returns **404 Not Found**.

## Endpoints and input rules

| Method | Path | Request body | Success |
| --- | --- | --- | --- |
| GET | `/parking` | None; use default location | 200 occupancy with location ID, available/occupied counts, and observation timestamp |
| GET | `/parking/{locationId}` | None | 200 occupancy for that location |
| POST | `/parking` | `vehicleReg`, `vehicleType`; use default location | 201 parking session |
| POST | `/parking/{locationId}` | `vehicleReg`, `vehicleType` | 201 parking session at that location |
| POST | `/parking/bill` | `vehicleReg` only | 200 completed session and final bill |

There are no query parameters. Departure finds the globally active registration; it does not take a location, session ID, or timestamp in its request body.

Vehicle type is `1` (small), `2` (medium), or `3` (large). Registrations follow the selected simplified pattern of 1–2 ASCII letters, 2–3 digits, then 1–3 ASCII letters. Lowercase and supported whitespace are accepted; storage and responses use whitespace-free uppercase values. This is an application assumption, not a complete validator for every UK registration format.

Mapped rejections use `application/problem+json`, with `status`, `title`, and `detail`:

- **400:** invalid request fields or malformed JSON.
- **404:** unknown location or no matching active session.
- **409:** duplicate active registration, full/closed location, parking retry exhaustion, or a departure reservation conflict.

Unexpected internal failures propagate to framework error handling; an explicit application Problem Details handler for those failures is not implemented.

## Configuration and charging

`api/src/main/resources/application.properties` imports two required files:

- `parking.properties`: startup locations, default location, and parking retry budget.
- `billing.properties`: rates in integer pence, interval seconds, and interval charge.

The supplied default location is ID **1**, `TDS Car Park 1`, capacity **10**, status **ACTIVE**. Each space can accept any supported vehicle type. Additional locations can be supplied through startup configuration; IDs must be explicit and unique. Location capacity/status and pricing remain fixed until restart. A CLOSED location rejects arrivals but still permits occupancy queries and departure.

| Vehicle type | Rate per minute |
| --- | --- |
| Small (`1`) | £0.10 |
| Medium (`2`) | £0.20 |
| Large (`3`) | £0.40 |

The additional charge is **£1 per completed five-minute interval**. Base charges use whole elapsed seconds and round down to whole pence; fractional seconds are ignored. For a small vehicle: 59 seconds costs £0.09, one minute £0.10, and five minutes £1.50. Charging continues across midnight and beyond 24 hours. Negative durations and arithmetic overflow are rejected.

Money arithmetic uses integer pence and `BigDecimal` internally. The public bill uses the brief's `double` representation. Entry, exit, and observation timestamps are UTC instants.

Spring external configuration can override the defaults. For example:

```sh
java -jar api/target/api-0.0.1-SNAPSHOT-exec.jar \
  --server.port=9090 \
  --billing.small-rate-pence-per-minute=15
```

The default parking retry budget is five retries after the initial attempt. Departure reservation conflicts return immediately without retrying.

## Testing and scope

The `api` module contains JUnit/AssertJ tests for services, repositories, and selected Spring configuration. Mockito is used for focused dependency-failure paths. The `acceptance-tests` module uses Cucumber and real HTTP requests to arrange vehicles and verify responses and subsequent occupancy. It keeps independent client DTOs, a fresh application context per scenario, and controlled event time through the injected test clock. Concurrent parking cases are coordinated within separate concurrency steps.

The full Maven build passes, including **67 API tests and 31 acceptance scenarios**, with no failures, errors, or skipped tests. Dedicated concurrent-departure HTTP coverage and additional configuration-validation cases are deferred. See the [design's limitations](documents/parking-api-solution-design.md#current-limitations-and-deferred-work) for the precise boundary.

Completed history is **unbounded and in memory**. It is lost on restart and can grow for the process lifetime. No history API or cleanup task runs.

The submission has no persistent store, authentication, payment processing, running-bill endpoint, history export, or Docker packaging. The original brief does not require Docker. Separate application processes do not share state.

## Future development options

- **Docker packaging:** provide a Java 21 runtime image for the executable JAR, with documented build/run commands.
- **Quartz scheduled history cleanup:** remove completed stays by departure age, with configurable retention periods and cleanup frequency. Optional soft per-location limits and location-filtered jobs could control growth. Cleanup must coordinate with in-progress departures so it cannot delete uncommitted history.

See the [design's future extension options](documents/parking-api-solution-design.md#future-extension-options) for further details.
