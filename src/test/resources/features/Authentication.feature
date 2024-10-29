Feature: User Authentication

  Scenario: User Register with valid data
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    When User Login with the credentials
    Then User is authenticated

  Scenario: User is already registered
    Given A user with the following credentials
      | user | email                       | password |
      | User | {String:10:$user}@email.com | password |
    And User is correctly registered
    When A user with the following credentials
      | user | email             | password |
      | User | {$user}@email.com | password |
    Then User receives a message that the email is already registered
