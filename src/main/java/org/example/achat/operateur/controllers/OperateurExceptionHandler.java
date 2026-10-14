package org.example.achat.operateur.controllers;

import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OperateurExceptionHandler {

	@ExceptionHandler(OperateurNotFoundException.class)
	public ProblemDetail handleNotFound(OperateurNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}
}
