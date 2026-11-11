Feature: Chatbot

  # Runs by default and in CI, where the app starts with features.chatbot.enabled=false.
  @chatbot-disabled
  Scenario: Chatbot is not available while the feature flag is off
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends the chat message "Hello"
    Then User receives a chat error with code 1004

  # The scenarios below need the app running with FEATURES_CHATBOT_ENABLED=true and a real GOOGLE_AI_API_KEY.
  # Run them with: ./gradlew e2eTest "-Dcucumber.filter.tags=@chatbot-live"
  @chatbot-live
  Scenario: User receives an answer
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends the chat message "Hello, what can you do?"
    Then User receives an answer in a new conversation

  @chatbot-live
  Scenario: Follow-up messages keep the context of the conversation
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends the chat message "Hi, my name is Ana. Please remember it."
    And User sends the chat message "What is my name? Answer with just the name." in the same conversation
    Then the answer mentions "Ana"

  @chatbot-live
  Scenario: Users cannot continue another user's conversation
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    And User sends the chat message "Hello"
    And Another user is registered and logged in
    When User sends the chat message "Hello" in the same conversation
    Then User receives a chat error with code 3002

  @chatbot-live
  Scenario: Unknown conversations are not found
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends the chat message "Hello" in an unknown conversation
    Then User receives a chat error with code 3002

  @chatbot-live
  Scenario: Blank messages are rejected
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends a blank chat message
    Then User receives a chat error with code 3001

  @chatbot-live
  Scenario: Messages over 4000 characters are rejected
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends a chat message of 4001 characters
    Then User receives a chat error with code 3001

  @chatbot-live
  Scenario: The assistant uses the calculator tool for arithmetic
    Given A user with the following credentials
      | user | email                 | password |
      | User | {String:10}@email.com | password |
    And User is correctly registered
    And User Login with the credentials
    When User sends the chat message "What is the square root of 144?"
    Then the answer mentions "12"
    And the assistant used the tool "calculate" with operation "sqrt"
