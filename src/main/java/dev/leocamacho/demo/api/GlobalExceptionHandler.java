package dev.leocamacho.demo.api;

import dev.leocamacho.demo.api.responses.ErrorResponse;
import dev.leocamacho.demo.exception.BusinessException;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static dev.leocamacho.demo.exception.ErrorCode.UNKNOWN_ERROR;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                ex.getErrorCode().code(),
                SessionContextHolder.getSession().correlationId(),
                ex.getParams().stream().map(String::valueOf).toArray(String[]::new)
        );
        LOGGER.error("A BusinessException occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleException(Throwable ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                UNKNOWN_ERROR.code(),
                SessionContextHolder.getSession().correlationId()
        );
        LOGGER.error("An uncontrolled error occurred", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
