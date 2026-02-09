Feature: Health endpoint

  Scenario: health check works
    When I call the health endpoint
    Then the response status should be 200