Feature: Appointment confirmation email (outbox)

  Scenario: Creating an appointment sends a confirmation email via outbox dispatcher
    Given the following doctors exist:
      | externalId | name              | specialty   |
      | doc-1      | Dr. João Silva    | Cardiology  |
    And the following rooms exist:
      | externalId | name              |
      | room-1     | Sala Alfa         |
    When I create an appointment to trigger email:
      | patientName | patientEmail           | specialty    | startTime             | endTime               |
      | Ana Costa   | ana@a.com              | Cardiology   | 2026-02-11T10:00:00Z  | 2026-02-11T10:30:00Z  |
    Then when I dispatch the outbox once, an email should be sent to "ana@a.com"