package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.ConcurrentParkingAttempt;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkRequest;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkResponse;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Sends concurrent HTTP bookings and checks successful allocations without inspecting application internals. */
public class ParkingConcurrencySteps {

    private final AtHelper helper;
    private final ScenarioContext context;

    public ParkingConcurrencySteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("vehicles attempt to park concurrently with:")
    public void vehiclesAttemptToParkConcurrently(DataTable table) throws Exception {
        List<BookingRequest> requests = parseRequests(table);
        assertThat(requests).as("at least two competing HTTP requests").hasSizeGreaterThanOrEqualTo(2);
        for (BookingRequest request : requests) {
            helper.configuredLocation(request.locationId());
            // Fixture counts must be recorded before dispatch; worker threads never access scenario state.
            context.expectedActiveStays(request.locationId());
        }
        context.recordConcurrentParkingAttempts(bookTogether(requests));
    }

    @Then("exactly {int} concurrent parking attempts succeed")
    public void exactlyConcurrentParkingAttemptsSucceed(int expected) {
        assertThat(successfulAttempts()).as("HTTP 201 booking responses").hasSize(expected);
    }

    @Then("between {int} and {int} concurrent parking attempts succeed")
    public void concurrentParkingSuccessCountIsWithinCapacity(int minimum, int maximum) {
        assertThat(successfulAttempts().size()).as("successful bookings within the available capacity")
                .isBetween(minimum, maximum);
    }

    @Then("successful concurrent bookings have valid unique sessions and spaces")
    public void successfulBookingsHaveValidUniqueSessionsAndSpaces() {
        List<ConcurrentParkingAttempt> successes = successfulAttempts();
        assertThat(successes).as("successful HTTP bookings to validate").isNotEmpty();
        assertThat(successes).allSatisfy(this::assertValidBooking);
        assertThat(successes).extracting(attempt -> successBody(attempt).parkingSessionId())
                .as("session IDs are unique across all bookings").doesNotHaveDuplicates();
        assertThat(successes).extracting(attempt -> successBody(attempt).vehicleReg())
                .as("active registrations are unique across all locations").doesNotHaveDuplicates();
        assertThat(successes)
                .extracting(ConcurrentParkingAttempt::locationId, attempt -> successBody(attempt).spaceNumber())
                .as("each location-space pair is allocated only once").doesNotHaveDuplicates();
    }

    @Then("the concurrent parking allocations are:")
    public void concurrentParkingAllocationsAre(DataTable table) {
        var expected = table.asMaps(String.class, String.class).stream()
                .map(row -> tuple(Long.parseLong(row.get("locationId")), row.get("vehicleReg"),
                        Integer.parseInt(row.get("spaceNumber"))))
                .toList();
        assertThat(successfulAttempts())
                .extracting(ConcurrentParkingAttempt::locationId,
                        attempt -> successBody(attempt).vehicleReg(),
                        attempt -> successBody(attempt).spaceNumber())
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private List<ConcurrentParkingAttempt> bookTogether(List<BookingRequest> requests) throws Exception {
        var start = new CyclicBarrier(requests.size());
        ExecutorService workers = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<ConcurrentParkingAttempt>> futures = new ArrayList<>();
            for (BookingRequest request : requests) {
                futures.add(workers.submit(() -> {
                    // The barrier creates the concurrent start: all workers wait until everyone is ready.
                    // Five seconds is the maximum wait, not a delay between requests.
                    start.await(5, SECONDS);
                    return new ConcurrentParkingAttempt(request.locationId(), request.body(),
                            helper.attemptToPark("/parking/" + request.locationId(), request.body()));
                }));
            }
            List<ConcurrentParkingAttempt> attempts = new ArrayList<>();
            for (Future<ConcurrentParkingAttempt> future : futures) {
                // Transport and coordination failures fail the scenario; they are not business rejections.
                attempts.add(future.get(15, SECONDS));
            }
            return attempts;
        } finally {
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, SECONDS)).as("concurrent HTTP workers stop").isTrue();
        }
    }

    private List<BookingRequest> parseRequests(DataTable table) {
        return table.asMaps(String.class, String.class).stream().map(this::parseRequest).toList();
    }

    private BookingRequest parseRequest(Map<String, String> row) {
        assertThat(row).containsOnlyKeys("locationId", "vehicleReg", "vehicleType");
        return new BookingRequest(Long.parseLong(row.get("locationId")),
                new PostVehicleParkRequest(row.get("vehicleReg"), Integer.parseInt(row.get("vehicleType"))));
    }

    private List<ConcurrentParkingAttempt> successfulAttempts() {
        return context.concurrentParkingAttempts().stream()
                .filter(attempt -> attempt.response().getStatusCode().equals(HttpStatus.CREATED)).toList();
    }

    private void assertValidBooking(ConcurrentParkingAttempt attempt) {
        assertThat(attempt.response().getHeaders().getContentType()).isNotNull()
                .satisfies(type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
        PostVehicleParkResponse body = successBody(attempt);
        assertThat(body.parkingSessionId()).as("session UUID").isNotNull();
        assertThat(body.timeIn()).as("entry timestamp").isNotNull();
        assertThat(body.vehicleReg()).as("normalised requested registration")
                .isEqualTo(attempt.request().vehicleReg().replaceAll("\\s+", "").toUpperCase(Locale.ROOT));
        assertThat(body.spaceNumber()).as("valid space at the requested location")
                .isBetween(1, helper.configuredLocation(attempt.locationId()).maxCapacity());
        // The Given fixtures occupy spaces 1 through their recorded stay count.
        assertThat(body.spaceNumber()).as("allocation does not reuse a fixture's occupied space")
                .isGreaterThan(context.expectedActiveStays(attempt.locationId()));
    }

    private PostVehicleParkResponse successBody(ConcurrentParkingAttempt attempt) {
        assertThat(attempt.response().getBody()).as("successful booking response")
                .isInstanceOf(PostVehicleParkResponse.class);
        return (PostVehicleParkResponse) attempt.response().getBody();
    }

    /** Holds a request before dispatch without sharing mutable scenario state with workers. */
    private record BookingRequest(long locationId, PostVehicleParkRequest body) {}
}
