Feature: Appointment creation

  Scenario: Create an appointment successfully
    Given the following doctors exist:
      | externalId | name              | specialty   |
      | doc-1      | Dr. João Silva    | Cardiology  |
    And the following rooms exist:
      | externalId | name              |
      | room-1     | Sala Alfa         |
    When I create an appointment:
      | patientName   | patientEmail       | specialty   | startTime                | endTime                  |
      | João Pereira  | joao@example.com   | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
    Then the appointment should be created successfully

  Scenario: Error when creating an appointment for an already booked doctor slot
    Given the following doctors exist:
      | externalId | name              | specialty   |
      | doc-1      | Dr. João Silva    | Cardiology  |
    And the following rooms exist:
      | externalId | name              |
      | room-1     | Sala Alfa         |
      | room-2     | Sala Beta         |
    When I create an appointment:
      | patientName    | patientEmail    | specialty   | startTime                | endTime                  |
      | Maria Santos   | maria@a.com     | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
    And I create an appointment:
      | patientName    | patientEmail    | specialty   | startTime                | endTime                  |
      | Ana Costa      | ana@a.com       | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
    Then I should get an appointment creation error "No availability (doctor or room) for the requested timeslot"