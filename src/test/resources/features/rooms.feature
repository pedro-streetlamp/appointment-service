Feature: Room information access

  Scenario: Fetch all rooms returns all rooms ordered by name
    Given the following rooms exist:
      | externalId | name   |
      | room-1     | Alpha  |
      | room-2     | Beta   |
      | room-3     | Gamma  |
    When I fetch all rooms
    Then I should get rooms:
      | externalId | name   |
      | room-1     | Alpha  |
      | room-2     | Beta   |
      | room-3     | Gamma  |