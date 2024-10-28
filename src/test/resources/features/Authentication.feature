Feature: User Authentication

  Scenario: User Register with valid data
    Given I have a user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And I am correctly registered
    When User Login with the credentials
    Then I have the user authenticated

  Scenario: User is already registered
    Given I have a user with the following credentials
      | user | email                       | password |
      | User | {String:10:$user}@email.com | password |
    And I am correctly registered
    When I have a user with the following credentials
      | user | email             | password |
      | User | {$user}@email.com | password |
    Then I receive a message that the email is already registered
