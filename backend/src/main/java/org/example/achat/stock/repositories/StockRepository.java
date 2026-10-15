package org.example.achat.stock.repositories;

import org.example.achat.stock.entities.Stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

	@Query("SELECT s FROM Stock s WHERE s.qte < s.qteMin")
	List<Stock> findStocksBelowMinimumQuantity();
}
