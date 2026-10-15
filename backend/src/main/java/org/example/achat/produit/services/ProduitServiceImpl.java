package org.example.achat.produit.services;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;

import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.repositories.ProduitRepository;

import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.repositories.StockRepository;
import org.example.achat.produit.exceptions.ProduitNotFoundException;
import org.example.achat.stock.exceptions.StockNotFoundException;


import java.util.List;

@Service
@Slf4j
public class ProduitServiceImpl implements IProduitService {

	private final ProduitRepository produitRepository;
	private final StockRepository stockRepository;
	private final CategorieProduitRepository categorieProduitRepository;

	public ProduitServiceImpl(
			ProduitRepository produitRepository,
			StockRepository stockRepository,
			CategorieProduitRepository categorieProduitRepository) {
		this.produitRepository = produitRepository;
		this.stockRepository = stockRepository;
		this.categorieProduitRepository = categorieProduitRepository;
	}

	@Override
	public List<Produit> retrieveAllProduits() {
		List<Produit> produits = produitRepository.findAll();
		log.info("Retrieved {} products", produits.size());
		return produits;
	}

	@Transactional
	public Produit addProduit(Produit p) {
		log.debug("Adding product '{}'", p.getLibelleProduit());
		Produit savedProduit = produitRepository.save(p);
		log.info("Product '{}' added with id {}", savedProduit.getLibelleProduit(), savedProduit.getIdProduit());
		return savedProduit;
	}

	

	@Override
	public void deleteProduit(Long produitId) {
		if (!produitRepository.existsById(produitId)) {
			log.warn("Cannot delete product {} because it does not exist", produitId);
			throw new ProduitNotFoundException(produitId);
		}
		produitRepository.deleteById(produitId);
		log.info("Deleted product {}", produitId);
	}

	@Override
	public Produit updateProduit(Produit p) {
		Produit existingProduit = produitRepository.findById(p.getIdProduit())
				.orElseThrow(() -> {
					log.warn("Cannot update product {} because it does not exist", p.getIdProduit());
					return new ProduitNotFoundException(p.getIdProduit());
				});
		existingProduit.setCodeProduit(p.getCodeProduit());
		existingProduit.setLibelleProduit(p.getLibelleProduit());
		existingProduit.setPrix(p.getPrix());
		existingProduit.setDateDerniereModification(p.getDateDerniereModification());
		Produit updatedProduit = produitRepository.save(existingProduit);
		log.info("Updated product {}", updatedProduit.getIdProduit());
		return updatedProduit;
	}

	@Override
	public Produit retrieveProduit(Long produitId) {
		return produitRepository.findById(produitId)
				.orElseThrow(() -> {
					log.warn("Product {} was not found", produitId);
					return new ProduitNotFoundException(produitId);
				});
	}

	@Override
	public void assignProduitToStock(Long idProduit, Long idStock) {
		log.debug("Assigning product {} to stock {}", idProduit, idStock);
		Produit produit = produitRepository.findById(idProduit)
				.orElseThrow(() -> {
					log.warn("Cannot assign product to stock because product {} was not found", idProduit);
					return new ProduitNotFoundException(idProduit);
				});
		Stock stock = stockRepository.findById(idStock)
				.orElseThrow(() -> {
					log.warn("Cannot assign product {} to stock because stock {} was not found", idProduit, idStock);
					return new StockNotFoundException(idStock);
				});
		produit.setStock(stock);
		produitRepository.save(produit);
		log.info("Assigned product {} to stock {}", idProduit, idStock);

	}


}