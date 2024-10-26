package dev.leocamacho.demo.api.exceptions;

import dev.leocamacho.demo.api.types.ErrorResponse;
import dev.leocamacho.demo.models.BaseException;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static dev.leocamacho.demo.models.ErrorCodes.UNKNOWN_ERROR;

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

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleException(Throwable ex) {

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                UNKNOWN_ERROR,
                //TODO update this for an anonnymous user
                SessionContextHolder.getSession() == null ? null : SessionContextHolder.getSession().correlationId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
