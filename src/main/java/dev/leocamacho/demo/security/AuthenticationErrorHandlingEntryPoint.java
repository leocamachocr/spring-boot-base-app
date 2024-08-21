package dev.leocamacho.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import dev.leocamacho.demo.api.types.ErrorResponse;
import dev.leocamacho.demo.models.ErrorCodes;
import dev.leocamacho.demo.session.SessionContextHolder;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class AuthenticationErrorHandlingEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Unauthorized", ErrorCodes.UNAUTHORIZED, SessionContextHolder.getSession().correlationId());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        OutputStream out = response.getOutputStream();
        objectMapper.writeValue(out, errorResponse);
        out.flush();
    }
}