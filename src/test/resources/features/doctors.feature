Feature: Doctor information access

  Scenario: Find doctors by specialty returns matching doctors ordered by name
    Given the following doctors exist:
      | externalId | name           | specialty   |
      | doc-1      | Ana Cardoso    | Cardiology  |
      | doc-2      | Bruno Almeida  | Cardiology  |
      | doc-3      | Carla Sousa    | Dermatology |
    When I search doctors by specialty "Cardiology"
    Then I should get doctors:
      | externalId | name           | specialty  |
      | doc-1      | Ana Cardoso    | Cardiology |
      | doc-2      | Bruno Almeida  | Cardiology |