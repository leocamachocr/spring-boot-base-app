package dev.leocamacho.demo.tests.handlers.commands;

import dev.leocamacho.demo.handlers.commands.EncodePasswordHandler;
import dev.leocamacho.demo.handlers.commands.RegisterUserHandler;
import dev.leocamacho.demo.handlers.commands.impl.RegisterUserHandlerImpl;
import dev.leocamacho.demo.persistence.model.UserEntity;
import dev.leocamacho.demo.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static dev.leocamacho.demo.persistence.model.UserEntity.UserEntityBuilder.anUserEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class RegisterUserHandlerTests {

    @Mock(strictness = Mock.Strictness.STRICT_STUBS)
    private UserRepository repository;
    @Mock(strictness = Mock.Strictness.STRICT_STUBS)
    private EncodePasswordHandler encodePasswordHandler;
    @InjectMocks
    private RegisterUserHandlerImpl registerUserHandler;


    @Test
    public void registerUserHandlerShouldReturnSuccess() {
        // Given
        var command = new RegisterUserHandler.Command(
                "Alice",
                "valid@username.com",
                "password"
        );
        var saved = anUserEntity()
                .withEmail("valid@username.com")
                .withName("Alice")
                .withPassword("password")
                .build();
        saved.setId(UUID.randomUUID());
        when(repository.save(any())).thenReturn(saved);
        when(encodePasswordHandler.handle(any()))
                .thenReturn(
                        new EncodePasswordHandler.Result
                        .Success("encodedPassword")
                );
        // When
        var result = registerUserHandler.handle(command);

        // Then
        assertEquals(new RegisterUserHandler.Result.Success(saved.getId()), result);
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals("Alice", captor.getValue().getName());
        assertEquals("encodedPassword", captor.getValue().getPassword());
        assertEquals("valid@username.com", captor.getValue().getEmail());
    }

    @Test
    public void registerUserHandlerShouldReturnInvalidFields() {
        // Given
        var command = new RegisterUserHandler.Command(null, null, null);
        // When
        var result = registerUserHandler.handle(command);

        // Then
        assertEquals(result.getClass(), RegisterUserHandler.Result.InvalidFields.class);
    }


}
