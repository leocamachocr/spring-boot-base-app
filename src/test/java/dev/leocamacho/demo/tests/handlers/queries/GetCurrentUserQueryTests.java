package dev.leocamacho.demo.tests.handlers.queries;

import dev.leocamacho.demo.handlers.queries.GetCurrentUserQuery;
import dev.leocamacho.demo.handlers.queries.impl.GetCurrentUserQueryImpl;
import dev.leocamacho.demo.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static dev.leocamacho.demo.persistence.model.UserEntity.UserEntityBuilder.anUserEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetCurrentUserQueryTests {

    @Mock
    private UserRepository repository;
    @InjectMocks
    private GetCurrentUserQueryImpl getCurrentUserQuery;

    @Test
    public void getCurrentUserShouldReturnMappedUser() {
        // Given
        var user = anUserEntity().withName("Alice").withEmail("alice@mail.com").withPassword("hash").build();
        user.setId(UUID.randomUUID());
        when(repository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        // When
        var result = getCurrentUserQuery.query("alice@mail.com");

        // Then
        assertEquals(new GetCurrentUserQuery.Result.Success(user.getId(), "Alice", "alice@mail.com"), result);
    }

    @Test
    public void getCurrentUserShouldReturnNotFoundForUnknownEmail() {
        // Given
        when(repository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        // When
        var result = getCurrentUserQuery.query("nobody@mail.com");

        // Then
        assertEquals(new GetCurrentUserQuery.Result.UserNotFound(), result);
    }

    @Test
    public void getCurrentUserShouldReturnNotFoundForNullEmail() {
        // When
        var result = getCurrentUserQuery.query(null);

        // Then
        assertEquals(new GetCurrentUserQuery.Result.UserNotFound(), result);
        verifyNoInteractions(repository);
    }
}
