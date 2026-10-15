package org.example.achat.fournisseurs.controllers;

import org.example.achat.fournisseurs.exceptions.FournisseurNotFoundException;
import org.example.achat.fournisseurs.exceptions.SecteurActiviteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FournisseurExceptionHandler {

	@ExceptionHandler({
			FournisseurNotFoundException.class,
			SecteurActiviteNotFoundException.class
	})
	public ProblemDetail handleNotFound(RuntimeException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}
}
