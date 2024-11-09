package dev.leocamacho.demo.api.exceptions;

import dev.leocamacho.demo.api.types.ErrorResponse;
import dev.leocamacho.demo.models.BaseException;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static dev.leocamacho.demo.models.ErrorCode.UNKNOWN_ERROR;

@ControllerAdvice
public class ExceptionManagerController {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionManagerController.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BaseException ex) {

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                ex.getCode(),
                SessionContextHolder.getSession().correlationId(),
                ex.getParams().toArray(new String[0])
        );
        logger.error("A BaseException occurred", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleException(Throwable ex) {


        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                UNKNOWN_ERROR.code(),
                SessionContextHolder.getSession().correlationId()
        );

        logger.error("An uncontrolled error occurred", ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
