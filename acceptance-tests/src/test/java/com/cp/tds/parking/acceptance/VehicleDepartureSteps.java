package com.cp.tds.parking.acceptance;

import com.cp.tds.parking.acceptance.dto.PostVehicleDepartRequest;
import com.cp.tds.parking.acceptance.dto.PostVehicleDepartResponse;
import com.cp.tds.parking.acceptance.dto.PostVehicleParkResponse;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sends departure HTTP requests at optional scenario-supplied times and records their responses.
 */
public class VehicleDepartureSteps {
    private final AtHelper helper;

    private final ScenarioContext context;

    public VehicleDepartureSteps(AtHelper helper, ScenarioContext context) {
        this.helper = helper;
        this.context = context;
    }

    @When("a vehicle attempts to depart with:")
    public void whenAVehicleAttemptsToDepartWith(DataTable dataTable) {
        attemptToDepartWithTable(dataTable);
    }

    @And("the final bill retains the original parking session ID")
    public void finalBillRetainsOriginalParkingSessionId() {
        // Preserve both HTTP responses in the shared scenario context so we can
        // verify that departure completes the same session created by parking.
        assertThat(context.vehicleDepartAttemptResponse().getBody())
                .as("final bill response body")
                .isInstanceOf(PostVehicleDepartResponse.class)
                .extracting("parkingSessionId")
                .as("final bill parking session ID")
                .isNotNull()
                .satisfies(departureSessionId ->
                        assertThat(context.vehicleParkingAttemptResponse().getBody())
                                .as("original parking response body")
                                .isInstanceOf(PostVehicleParkResponse.class)
                                .extracting("parkingSessionId")
                                .as("original parking session ID matches the final bill")
                                .isEqualTo(departureSessionId));
    }

    @And("the final bill contains:")
    public void whenAVehicleContainsTheFinalBill(DataTable dataTable) {
        var rows = dataTable.asMaps(String.class, String.class);
        assertThat(rows).as("one expected bill response").hasSize(1);
        var expected = rows.getFirst();
        assertThat(expected)
                .as("expected vehicle departure response fields")
                .containsOnlyKeys("vehicleReg", "vehicleCharge", "timeIn", "timeOut");

        assertThat(context.vehicleDepartAttemptResponse().getBody())
                .as("final bill response body")
                .isInstanceOf(PostVehicleDepartResponse.class)
                .extracting("vehicleReg", "vehicleCharge", "timeIn", "timeOut")
                .as("final bill registration, charge, entry time, and departure time")
                .containsExactly(
                        Objects.requireNonNull(expected.get("vehicleReg")),
                        Double.valueOf(Objects.requireNonNull(expected.get("vehicleCharge"))),
                        Instant.parse(Objects.requireNonNull(expected.get("timeIn"))),
                        Instant.parse(Objects.requireNonNull(expected.get("timeOut"))));
    }

    private void attemptToDepartWithTable(DataTable dataTable) {

        var rows = dataTable.asMaps(String.class, String.class);
        assertThat(rows).hasSize(1);

        var row = rows.getFirst();

        PostVehicleDepartRequest request = new PostVehicleDepartRequest(row.get("vehicleReg"));

        if (row.containsKey("timeOut")) {
            helper.setRequestTime(Instant.parse(Objects.requireNonNull(row.get("timeOut"))));
        }

        attemptToDepart(request);
    }

    private void attemptToDepart(Object requestBody) {
        // departures are always sent to /parking/bill. Location is found by the ActiveStay.
        context.recordVehicleDepartResponse(helper.attemptToDepart("/parking/bill", requestBody));
    }
}
