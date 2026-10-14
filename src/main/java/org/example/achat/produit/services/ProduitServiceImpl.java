package org.example.achat.produit.services;

import org.springframework.stereotype.Service;

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
		return produitRepository.findAll();
	}

	@Transactional
	public Produit addProduit(Produit p) {
		return produitRepository.save(p);
	}

	

	@Override
	public void deleteProduit(Long produitId) {
		if (!produitRepository.existsById(produitId)) {
			throw new ProduitNotFoundException(produitId);
		}
		produitRepository.deleteById(produitId);
	}

	@Override
	public Produit updateProduit(Produit p) {
		Produit existingProduit = produitRepository.findById(p.getIdProduit())
				.orElseThrow(() -> new ProduitNotFoundException(p.getIdProduit()));
		existingProduit.setCodeProduit(p.getCodeProduit());
		existingProduit.setLibelleProduit(p.getLibelleProduit());
		existingProduit.setPrix(p.getPrix());
		existingProduit.setDateDerniereModification(p.getDateDerniereModification());
		return produitRepository.save(existingProduit);
	}

	@Override
	public Produit retrieveProduit(Long produitId) {
		return produitRepository.findById(produitId)
				.orElseThrow(() -> new ProduitNotFoundException(produitId));
	}

	@Override
	public void assignProduitToStock(Long idProduit, Long idStock) {
		Produit produit = produitRepository.findById(idProduit)
				.orElseThrow(() -> new ProduitNotFoundException(idProduit));
		Stock stock = stockRepository.findById(idStock)
				.orElseThrow(() -> new StockNotFoundException(idStock));
		produit.setStock(stock);
		produitRepository.save(produit);

	}


}