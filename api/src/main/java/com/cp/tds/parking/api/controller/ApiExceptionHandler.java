package com.cp.tds.parking.api.controller;

import com.cp.tds.parking.api.exception.LocationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates application exceptions into HTTP Problem Detail responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LocationNotFoundException.class)
    public ProblemDetail handleLocationNotFoundException(LocationNotFoundException lnfe) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                lnfe.getMessage()
        );
    }

}
