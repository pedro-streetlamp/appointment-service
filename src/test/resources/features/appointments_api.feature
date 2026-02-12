Feature: Appointments API

  Scenario: Create an appointment successfully via API
    Given the following doctors exist:
      | externalId | name           | specialty  |
      | doc-1      | Dr. João Silva | Cardiology |
    And the following rooms exist:
      | externalId | name      |
      | room-1     | Sala Alfa |
    When I create an appointment via API:
      | patientName  | patientEmail     | specialty  | startTime            | endTime              |
      | João Pereira | joao@example.com | Cardiology | 2030-01-01T10:00:00Z | 2030-01-01T10:30:00Z |
    Then the API response status should be 201
    And the appointment should be created in the database

  Scenario: Error when creating an appointment for an already booked doctor slot via API
    Given the following doctors exist:
      | externalId | name           | specialty  |
      | doc-1      | Dr. João Silva | Cardiology |
    And the following rooms exist:
      | externalId | name      |
      | room-1     | Sala Alfa |
      | room-2     | Sala Beta |
    When I create an appointment via API:
      | patientName  | patientEmail  | specialty  | startTime            | endTime              |
      | Maria Santos | maria@a.com   | Cardiology | 2030-01-01T10:00:00Z | 2030-01-01T10:30:00Z |
    Then the API response status should be 201
    When I create an appointment via API:
      | patientName | patientEmail | specialty  | startTime            | endTime              |
      | Ana Costa   | ana@a.com    | Cardiology | 2030-01-01T10:00:00Z | 2030-01-01T10:30:00Z |
    Then the API response status should be 409
    And I should get an API error message "No availability (doctor or room) for the requested timeslot"

  Scenario: List appointments requires admin JWT
    When I list appointments via API
    Then the API response status should be 401

    When I list appointments via API as non-admin
    Then the API response status should be 403

    When I list appointments via API as admin
    Then the API response status should be 200