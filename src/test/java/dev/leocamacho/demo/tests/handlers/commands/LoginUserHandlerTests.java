package dev.leocamacho.demo.tests.handlers.commands;

import dev.leocamacho.demo.handlers.commands.LoginUserHandler;
import dev.leocamacho.demo.handlers.commands.impl.LoginUserHandlerImpl;
import dev.leocamacho.demo.security.AuthenticatedUser;
import dev.leocamacho.demo.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginUserHandlerTests {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtProvider jwtProvider;
    @InjectMocks
    private LoginUserHandlerImpl loginUserHandler;

    @Test
    public void loginUserHandlerShouldReturnSuccess() {
        // Given
        var user = new AuthenticatedUser(UUID.randomUUID(), "Alice", "alice@mail.com", "hash", List.of());
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(jwtProvider.generateToken(user)).thenReturn("token");

        // When
        var result = loginUserHandler.handle(new LoginUserHandler.Command("alice@mail.com", "password"));

        // Then
        assertEquals(new LoginUserHandler.Result.Success("token", "Alice", "alice@mail.com"), result);
    }

    @Test
    public void loginUserHandlerShouldReturnInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // When
        var result = loginUserHandler.handle(new LoginUserHandler.Command("alice@mail.com", "wrong"));

        // Then
        assertEquals(new LoginUserHandler.Result.InvalidCredentials(), result);
        verifyNoInteractions(jwtProvider);
    }
}
