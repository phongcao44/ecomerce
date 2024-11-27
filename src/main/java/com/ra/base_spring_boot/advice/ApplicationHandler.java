package com.ra.base_spring_boot.advice;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.exception.CustomException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ApplicationHandler
{

    /**
     * @param ex MethodArgumentNotValidException
     * @apiNote handle valid data in request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidException(MethodArgumentNotValidException ex)
    {
        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .code(400)
                        .data(errors)
                        .build()
        );
    }

    /**
     * @apiNote handle exception max upload file
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadFile()
    {
        return ResponseEntity.badRequest().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .code(400)
                        .data("File so big")
                        .build()
        );
    }

    /**
     * @param ex UsernameNotFoundException
     * @apiNote handle exception username not found in user detail service
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex)
    {
        return new ResponseEntity<>(
                ResponseWrapper.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .code(404)
                        .data(ex.getMessage())
                        .build()
                , HttpStatus.NOT_FOUND
        );
    }

    /**
     * @param ex CustomException
     * @apiNote handle custom exception
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex)
    {
        return new ResponseEntity<>(
                ResponseWrapper.builder()
                        .status(ex.getStatus())
                        .code(ex.getStatus().value())
                        .data(ex.getMessage())
                        .build(),
                ex.getStatus()
        );
    }

    /**
     * @param ex NoResourceFoundException
     * @apiNote handle not found exception with server no resource
     * */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceHandler(NoResourceFoundException ex) {
        return new ResponseEntity<>(
                ResponseWrapper.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .code(400)
                        .data(ex.getMessage())
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

}
