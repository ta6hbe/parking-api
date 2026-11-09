Feature: Parking vehicle

  Scenario: Park a vehicle at the default location when free spaces exist
    Given the default parking location has a capacity of 10
    And the default parking location starts empty
    When a vehicle attempts to park at the default location with:
      | vehicleReg | vehicleType |
      | ab 123 cde | 1           |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | AB123CDE   | 1           | TIMESTAMP |
    When I request its parking availability
    Then there should be 1 occupied spaces
    And there should be 9 available spaces

  Scenario Outline: Every supported vehicle type can park at an explicit location
    Given parking location 2 exists
    And parking location 2 has a capacity of 20
    And parking location 2 has 0 active stays
    When a vehicle attempts to park at location 2 with:
      | vehicleReg | vehicleType   |
      | XY12ABC    | <vehicleType> |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | XY12ABC    | 1           | TIMESTAMP |
    When I request parking availability for location 2
    Then there should be 1 occupied spaces
    And there should be 19 available spaces
    When I request its parking availability
    Then there should be 0 occupied spaces
    And there should be 10 available spaces

    Examples:
      | vehicleType |
      | 1           |
      | 2           |
      | 3           |

  Scenario: Allocate the first free space after existing stays
    Given parking location 1 has a capacity of 10
    And parking location 1 has 2 active stays
    When a vehicle attempts to park at the default location with:
      | vehicleReg | vehicleType |
      | AB12CDE    | 1           |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | AB12CDE    | 3           | TIMESTAMP |
    When I request its parking availability
    Then there should be 3 occupied spaces
    And there should be 7 available spaces

  Scenario Outline: Reject a registration already parked at any location
    Given parking location 1 has a capacity of 10
    And parking location 2 has a capacity of 20
    And parking location 1 has 1 active stay
    And parking location 2 has 0 active stays
    When a vehicle attempts to park at location <locationId> with:
      | vehicleReg    | vehicleType |
      | l101 car       | 1           |
    Then I get a 409 http response status
    And the response is a 409 problem containing "Duplicate"
    When I request parking availability for location 1
    Then there should be 1 occupied spaces
    When I request parking availability for location 2
    Then there should be 0 occupied spaces

    Examples:
      | locationId |
      | 1          |
      | 2          |

  Scenario: Reject parking when the location is full
    Given parking location 1 has a capacity of 10
    And parking location 1 has 10 active stays
    When a vehicle attempts to park at the default location with:
      | vehicleReg | vehicleType |
      | AB12CDE    | 1           |
    Then I get a 409 http response status
    And the response is a 409 problem containing "is full"
    When I request its parking availability
    Then there should be 10 occupied spaces
    And there should be 0 available spaces

  Scenario: Reject parking at a closed location
    Given parking location 3 exists
    And parking location 3 has a capacity of 10
    And parking location 3 has 0 active stays
    When a vehicle attempts to park at location 3 with:
      | vehicleReg | vehicleType |
      | AB12CDE    | 1           |
    Then I get a 409 http response status
    And the response is a 409 problem containing "closed"
    When I request parking availability for location 3
    Then there should be 0 occupied spaces

  Scenario: Reject parking at an unknown location
    Given parking location 999 is not configured
    When a vehicle attempts to park at location 999 with:
      | vehicleReg | vehicleType |
      | AB12CDE    | 1           |
    Then I get a 404 http response status
    And the response is a 404 problem containing "999"

  Scenario Outline: Reject invalid parking requests without occupying a space
    Given the default parking location starts empty
    When a vehicle attempts to park at the default location with JSON:
      """
      <request>
      """
    Then I get a 400 http response status
    And the response is a 400 problem containing "<problemDetail>"
    When I request its parking availability
    Then there should be 0 occupied spaces
    And there should be 10 available spaces

    Examples:
      | request                                      | problemDetail                                                    |
      | {"vehicleReg":"", "vehicleType":1}           | Request fields failed validation                                 |
      | {"vehicleReg":"   ", "vehicleType":1}        | Request fields failed validation                                 |
      | {"vehicleReg":"AB-12", "vehicleType":1}      | Request fields failed validation                                 |
      | {"vehicleReg":"ΑΒ123", "vehicleType":1}      | Request fields failed validation                                 |
      | {"vehicleReg":null, "vehicleType":1}         | Request fields failed validation                                 |
      | {"vehicleType":1}                            | Request fields failed validation                                 |
      | {"vehicleReg":"AB12CDE", "vehicleType":0}    | Request fields failed validation                                 |
      | {"vehicleReg":"AB12CDE", "vehicleType":4}    | Request fields failed validation                                 |
      | {"vehicleReg":"AB12CDE", "vehicleType":null} | Request fields failed validation                                 |
      | {"vehicleReg":"AB12CDE"}                     | Request fields failed validation                                 |
      | {"vehicleReg":                               | Request body must contain valid JSON with the expected field types |
