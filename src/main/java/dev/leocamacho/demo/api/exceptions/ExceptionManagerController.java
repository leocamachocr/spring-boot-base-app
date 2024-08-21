package dev.leocamacho.demo.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import dev.leocamacho.demo.api.types.ErrorResponse;
import dev.leocamacho.demo.models.BaseException;
import dev.leocamacho.demo.session.SessionContextHolder;

@ControllerAdvice
public class ExceptionManagerController {


    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BaseException ex) {

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                ex.getCode(),
                SessionContextHolder.getSession().correlationId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
