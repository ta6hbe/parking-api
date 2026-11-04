Feature: Parking occupancy

  Scenario: A configured car park starts empty
    Given the default parking location has a capacity of 10
    When I request its parking availability
    Then there should be 10 available spaces
    And there should be 0 occupied spaces
