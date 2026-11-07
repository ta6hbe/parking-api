Feature: Concurrent vehicle parking

  Scenario: The same registration cannot be booked concurrently at different locations
    Given parking location 1 has 0 active stays
    And parking location 2 has 0 active stays
    When vehicles attempt to park concurrently with:
      | locationId | vehicleReg | vehicleType |
      | 1          | ab12 cde   | 1           |
      | 2          | AB12CDE    | 2           |
    Then exactly 1 concurrent parking attempts succeed
    And 1 concurrent parking attempts are rejected with a 409 problem containing "Duplicate"
    And successful concurrent bookings have valid unique sessions and spaces
    And parking availability matches the successful concurrent bookings

  Scenario Outline: Competing vehicles cannot share a space or exceed location capacity
    Given parking location 1 has a capacity of 10
    And parking location 1 has <activeStays> active stays
    When vehicles attempt to park concurrently with:
      | locationId | vehicleReg | vehicleType |
      | 1          | AB12CDE    | 1           |
      | 1          | CD34EFG    | 2           |
      | 1          | EF56GHI    | 3           |
    Then between 1 and <freeSpaces> concurrent parking attempts succeed
    And all unsuccessful concurrent parking attempts are 409 problems
    And concurrent rejections report a full or busy location
    And successful concurrent bookings have valid unique sessions and spaces
    And parking availability matches the successful concurrent bookings

    # Leave one or two spaces for three competing vehicles; bounded retries may reject a busy request.
    Examples:
      | activeStays | freeSpaces |
      | 9           | 1          |
      | 8           | 2          |

  Scenario: Concurrent bookings at different locations may use the same space number
    Given parking location 1 has 0 active stays
    And parking location 2 has 0 active stays
    When vehicles attempt to park concurrently with:
      | locationId | vehicleReg | vehicleType |
      | 1          | AB12CDE    | 1           |
      | 2          | CD34EFG    | 2           |
    Then exactly 2 concurrent parking attempts succeed
    And successful concurrent bookings have valid unique sessions and spaces
    And the concurrent parking allocations are:
      | locationId | vehicleReg | spaceNumber |
      | 1          | AB12CDE    | 1           |
      | 2          | CD34EFG    | 1           |
    And parking availability matches the successful concurrent bookings

  Scenario: Concurrent duplicate rejections leave spaces available for other vehicles
    Given parking location 1 has 1 active stay
    And parking location 2 has 0 active stays
    When vehicles attempt to park concurrently with:
      | locationId | vehicleReg | vehicleType |
      | 1          | l101 car   | 1           |
      | 2          | L101CAR    | 2           |
    Then exactly 0 concurrent parking attempts succeed
    And 2 concurrent parking attempts are rejected with a 409 problem containing "Duplicate"
    And parking availability matches the successful concurrent bookings
    When a vehicle attempts to park at location 1 with:
      | vehicleReg | vehicleType |
      | XY12ABC    | 1           |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | XY12ABC    | 2           | TIMESTAMP |
    When I request parking availability for location 1
    Then there should be 2 occupied spaces
    And there should be 8 available spaces
    When a vehicle attempts to park at location 2 with:
      | vehicleReg | vehicleType |
      | XY34DEF    | 1           |
    Then I get a 201 http response status
    And post parking response contains:
      | parkingSessionId | vehicleReg | spaceNumber | timeIn    |
      | UUID             | XY34DEF    | 1           | TIMESTAMP |
    When I request parking availability for location 2
    Then there should be 1 occupied spaces
    And there should be 19 available spaces
