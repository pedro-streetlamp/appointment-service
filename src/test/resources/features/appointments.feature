Feature: Appointment creation

  Scenario: Create an appointment successfully
    Given the following doctors exist:
      | externalId | name              | specialty   |
      | doc-1      | Dr. João Silva    | Cardiology  |
    And the following rooms exist:
      | externalId | name              |
      | room-1     | Sala Alfa         |
    When I create an appointment:
      | doctorExternalId | roomExternalId | patientName   | patientEmail       | specialty   | startTime                | endTime                  |
      | doc-1            | room-1         | João Pereira  | joao@example.com   | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
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
      | doctorExternalId | roomExternalId | patientName    | patientEmail    | specialty   | startTime                | endTime                  |
      | doc-1            | room-1         | Maria Santos   | maria@a.com     | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
    And I create an appointment:
      | doctorExternalId | roomExternalId | patientName    | patientEmail    | specialty   | startTime                | endTime                  |
      | doc-1            | room-2         | Ana Costa      | ana@a.com       | Cardiology  | 2030-01-01T10:00:00Z     | 2030-01-01T10:30:00Z     |
    Then I should get an appointment creation error "Doctor is already booked for that time slot"