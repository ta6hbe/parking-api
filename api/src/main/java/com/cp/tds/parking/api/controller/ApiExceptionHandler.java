package com.cp.tds.parking.api.controller;

import com.cp.tds.parking.api.exception.ActiveStayNotFoundException;
import com.cp.tds.parking.api.exception.DepartureConflictException;
import com.cp.tds.parking.api.exception.DuplicateRegistrationException;
import com.cp.tds.parking.api.exception.LocationBusyException;
import com.cp.tds.parking.api.exception.LocationClosedException;
import com.cp.tds.parking.api.exception.LocationNotFoundException;
import com.cp.tds.parking.api.exception.LocationFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(ActiveStayNotFoundException.class)
    public ProblemDetail handleActiveStayNotFoundException(ActiveStayNotFoundException snfe) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                snfe.getMessage()
        );
    }

    @ExceptionHandler({
            LocationFullException.class,
            LocationBusyException.class,
            LocationClosedException.class,
            DuplicateRegistrationException.class,
            DepartureConflictException.class
    })
    public ProblemDetail handlerParkingConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body must contain valid JSON with the expected field types"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidRequest(
            MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request fields failed validation"
        );
    }

}
