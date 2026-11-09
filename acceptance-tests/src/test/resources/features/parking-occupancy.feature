Feature: Parking occupancy

  Scenario: A configured car park starts empty
    Given the default parking location has a capacity of 10
    When I request its parking availability
    Then there should be 10 available spaces
    And there should be 0 occupied spaces
    And the response should include an observation timestamp

  Scenario: An explicitly selected car park starts empty
    Given parking location 2 exists
    And parking location 2 has a capacity of 20
    When I request parking availability for location 2
    Then the number of available spaces should match its maximum capacity
    And there should be 0 occupied spaces
    And the response should include an observation timestamp

  Scenario: An explicitly selected car park starts with existing stays
    Given parking location 2 exists
    And parking location 1 exists
    And parking location 1 has a capacity of 10
    And parking location 2 has a capacity of 20
    And parking location 1 has 1 active stay
    And parking location 2 has 5 active stays
    When I request parking availability for location 2
    Then the number of available spaces should match its maximum capacity minus the active stays
    And there should be 5 occupied spaces
    And the response should include an observation timestamp

  Scenario: An unknown parking location returns a not-found problem
    Given parking location 999 is not configured
    When I request parking availability for an unknown location 999
    Then the response is a 404 problem containing "999"
