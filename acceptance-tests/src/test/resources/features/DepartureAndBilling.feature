Feature: Vehicle departure and billing

  Scenario: Depart a vehicle after five minutes and release its space
    Given parking location 1 has a capacity of 10
    When a vehicle attempts to park at location 1 with:
      | vehicleReg | vehicleType | timeIn               |
      | AB12CDE    | 1           | 2026-11-08T12:00:00Z |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | AB12CDE    | 1           | TIMESTAMP |
    And the parking entry time is "2026-11-08T12:00:00Z"
    When a vehicle attempts to depart with:
      | vehicleReg | timeOut              |
      | ab 12 cde  | 2026-11-08T12:05:00Z |
    Then I get a 200 http response status
    And the final bill retains the original parking session ID
    And the final bill contains:
      | vehicleReg | vehicleCharge | timeIn               | timeOut              |
      | AB12CDE    | 1.50          | 2026-11-08T12:00:00Z | 2026-11-08T12:05:00Z |
    When I request parking availability for location 1
    Then there should be 0 occupied spaces
    And there should be 10 available spaces
