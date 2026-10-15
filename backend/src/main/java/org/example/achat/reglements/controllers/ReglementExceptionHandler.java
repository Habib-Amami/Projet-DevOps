package org.example.achat.reglements.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.example.achat.reglements.exceptions.ReglementNotFoundException;

@RestControllerAdvice
public class ReglementExceptionHandler {

	@ExceptionHandler(ReglementNotFoundException.class)
	public ProblemDetail handleNotFound(ReglementNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
