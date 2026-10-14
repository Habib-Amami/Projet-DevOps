package org.example.achat.produit.controllers;

import org.example.achat.produit.exceptions.CategorieProduitNotFoundException;
import org.example.achat.produit.exceptions.ProduitNotFoundException;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProduitExceptionHandler {

	@ExceptionHandler({
			ProduitNotFoundException.class,
			CategorieProduitNotFoundException.class,
			StockNotFoundException.class
	})
	public ProblemDetail handleNotFound(RuntimeException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}
}
