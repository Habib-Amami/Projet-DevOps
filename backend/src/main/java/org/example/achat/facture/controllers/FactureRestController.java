package org.example.achat.facture.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.example.achat.facture.dto.FactureRequest;
import org.example.achat.facture.dto.FactureResponse;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.mapper.FactureMapper;
import org.example.achat.facture.services.IFactureService;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Date;
import java.util.List;


@RestController
@Tag(name = "Factures", description = "Invoice management")
@RequestMapping("/facture")
@CrossOrigin("*")
public class FactureRestController {

    @Autowired
    IFactureService factureService;
    @Autowired
    FactureMapper factureMapper;

    // http://localhost:8089/SpringMVC/facture/retrieve-all-factures
    @GetMapping("/retrieve-all-factures")
    @ResponseBody
    public List<FactureResponse> getFactures() {
        List<Facture> list = factureService.retrieveAllFactures();
        return factureMapper.toResponseList(list);
    }

    // http://localhost:8089/SpringMVC/facture/retrieve-facture/8
    @GetMapping("/retrieve-facture/{facture-id}")
    @ResponseBody
    public FactureResponse retrieveFacture(@PathVariable("facture-id") Long factureId) {
        return factureMapper.toResponse(factureService.retrieveFacture(factureId));
    }

    // http://localhost:8089/SpringMVC/facture/add-facture
    @PostMapping("/add-facture")
    @ResponseBody
    public FactureResponse addFacture(@Valid @RequestBody FactureRequest request) {
        return factureMapper.toResponse(factureService.createFacture(request));
    }

    /*
     * une facture peut etre annulé si elle a été saisie par erreur Pour ce
     * faire, il suffit de mettre le champs active à false
     */
    // http://localhost:8089/SpringMVC/facture/cancel-facture/{facture-id}
    @PutMapping("/cancel-facture/{facture-id}")
    @ResponseBody
    public void cancelFacture(@PathVariable("facture-id") Long factureId) {
        factureService.cancelFacture(factureId);
    }

    // http://localhost:8089/SpringMVC/facture/getFactureByFournisseur/{fournisseur-id}
    @GetMapping("/getFactureByFournisseur/{fournisseur-id}")
    @ResponseBody
    public List<FactureResponse> getFactureByFournisseur(@PathVariable("fournisseur-id") Long fournisseurId) {
        return factureMapper.toResponseList(factureService.getFacturesByFournisseur(fournisseurId));
    }

    // http://localhost:8089/SpringMVC/facture/assignOperateurToFacture/1/1
    @PutMapping(value = "/assignOperateurToFacture/{idOperateur}/{idFacture}")
    public void assignOperateurToFacture(@PathVariable("idOperateur") Long idOperateur, @PathVariable("idFacture") Long idFacture) {
        factureService.assignOperateurToFacture(idOperateur, idFacture);
    }

    // http://localhost:8089/SpringMVC/facture/pourcentageRecouvrement/{startDate}/{endDate}
    @GetMapping(value = "/pourcentageRecouvrement/{startDate}/{endDate}")
    public float pourcentageRecouvrement(
            @PathVariable(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @PathVariable(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        try {
            return factureService.pourcentageRecouvrement(startDate, endDate);
        } catch (Exception e) {
            return 0;
        }
    }

}
