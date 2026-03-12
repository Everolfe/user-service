package com.github.everolfe.userservice.exception;


import com.github.everolfe.userservice.dto.GetErrorDto;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<GetErrorDto> resourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(
                ex,
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<GetErrorDto> duplicateResource(
            DuplicateResourceException ex, WebRequest request){
        return buildErrorResponse(
                ex,
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request);
    }
    private ResponseEntity<GetErrorDto> buildErrorResponse(
            Exception ex, String message, HttpStatus status, WebRequest request) {

        GetErrorDto errorDto = new GetErrorDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorDto, status);
    }
}
