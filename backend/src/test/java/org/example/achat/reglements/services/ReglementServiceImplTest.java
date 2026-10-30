package org.example.achat.reglements.services;

import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.exceptions.ReglementNotFoundException;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReglementServiceImplTest {

	@Mock
	private ReglementRepository reglementRepository;

	@InjectMocks
	private ReglementServiceImpl reglementService;

	private Reglement buildReglement(Long id) {
		Reglement r = new Reglement();
		r.setIdReglement(id);
		r.setMontantPaye(100f);
		r.setMontantRestant(50f);
		r.setPayee(false);
		r.setDateReglement(new Date());
		return r;
	}

	// ---------- retrieveAllReglements ----------

	@Test
	@DisplayName("retrieveAllReglements retourne la liste du repository")
	void retrieveAllReglements_shouldReturnList() {
		List<Reglement> list = Arrays.asList(buildReglement(1L), buildReglement(2L));
		when(reglementRepository.findAll()).thenReturn(list);

		List<Reglement> result = reglementService.retrieveAllReglements();

		assertEquals(2, result.size());
		assertSame(list, result);
		verify(reglementRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("retrieveAllReglements retourne une liste vide")
	void retrieveAllReglements_shouldReturnEmptyList() {
		when(reglementRepository.findAll()).thenReturn(Collections.emptyList());

		List<Reglement> result = reglementService.retrieveAllReglements();

		assertTrue(result.isEmpty());
		verify(reglementRepository).findAll();
		verifyNoMoreInteractions(reglementRepository);
	}

	// ---------- addReglement ----------

	@Test
	@DisplayName("addReglement sauvegarde et retourne le règlement")
	void addReglement_shouldSaveAndReturn() {
		Reglement r = buildReglement(null);
		Reglement saved = buildReglement(1L);
		when(reglementRepository.save(r)).thenReturn(saved);

		Reglement result = reglementService.addReglement(r);

		assertNotNull(result);
		assertEquals(1L, result.getIdReglement());
		verify(reglementRepository).save(r);
	}

	// ---------- retrieveReglement ----------

	@Test
	@DisplayName("retrieveReglement retourne le règlement existant")
	void retrieveReglement_shouldReturnWhenFound() {
		Reglement r = buildReglement(1L);
		when(reglementRepository.findById(1L)).thenReturn(Optional.of(r));

		Reglement result = reglementService.retrieveReglement(1L);

		assertSame(r, result);
		verify(reglementRepository).findById(1L);
	}

	@Test
	@DisplayName("retrieveReglement lève ReglementNotFoundException si absent")
	void retrieveReglement_shouldThrowWhenNotFound() {
		when(reglementRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ReglementNotFoundException.class,
				() -> reglementService.retrieveReglement(99L));
		verify(reglementRepository).findById(99L);
	}

	// ---------- updateReglement ----------

	@Test
	@DisplayName("updateReglement met à jour les champs et sauvegarde")
	void updateReglement_shouldUpdateAndSave() {
		Reglement existing = buildReglement(1L);

		Reglement update = new Reglement();
		update.setIdReglement(1L);
		update.setMontantPaye(200f);
		update.setMontantRestant(0f);
		update.setPayee(true);
		Date newDate = new Date(System.currentTimeMillis() + 86_400_000L);
		update.setDateReglement(newDate);

		when(reglementRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(reglementRepository.save(existing)).thenReturn(existing);

		Reglement result = reglementService.updateReglement(update);

		assertEquals(200f, result.getMontantPaye());
		assertEquals(0f, result.getMontantRestant());
		assertTrue(result.getPayee());
		assertEquals(newDate, result.getDateReglement());
		verify(reglementRepository).findById(1L);
		verify(reglementRepository).save(existing);
	}

	@Test
	@DisplayName("updateReglement lève ReglementNotFoundException si absent")
	void updateReglement_shouldThrowWhenNotFound() {
		Reglement update = buildReglement(99L);
		when(reglementRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ReglementNotFoundException.class,
				() -> reglementService.updateReglement(update));
		verify(reglementRepository, never()).save(any(Reglement.class));
	}

	// ---------- deleteReglement ----------

	@Test
	@DisplayName("deleteReglement supprime quand l'id existe")
	void deleteReglement_shouldDeleteWhenExists() {
		when(reglementRepository.existsById(1L)).thenReturn(true);

		reglementService.deleteReglement(1L);

		verify(reglementRepository).existsById(1L);
		verify(reglementRepository).deleteById(1L);
	}

	@Test
	@DisplayName("deleteReglement lève ReglementNotFoundException si absent")
	void deleteReglement_shouldThrowWhenNotExists() {
		when(reglementRepository.existsById(99L)).thenReturn(false);

		assertThrows(ReglementNotFoundException.class,
				() -> reglementService.deleteReglement(99L));
		verify(reglementRepository, never()).deleteById(anyLong());
	}

	// ---------- findByFactureId ----------

	@Test
	@DisplayName("findByFactureId retourne les règlements de la facture")
	void findByFactureId_shouldReturnList() {
		List<Reglement> list = Arrays.asList(buildReglement(1L), buildReglement(2L));
		when(reglementRepository.findByFactureId(10L)).thenReturn(list);

		List<Reglement> result = reglementService.findByFactureId(10L);

		assertEquals(2, result.size());
		verify(reglementRepository).findByFactureId(10L);
	}

	// ---------- getRevenueBetweenDates ----------

	@Test
	@DisplayName("getRevenueBetweenDates retourne le revenu du repository")
	void getRevenueBetweenDates_shouldReturnRevenue() {
		Date start = new Date(1_000_000L);
		Date end = new Date(2_000_000L);
		when(reglementRepository.getRevenueBetweenDates(start, end)).thenReturn(1500.5f);

		float result = reglementService.getRevenueBetweenDates(start, end);

		assertEquals(1500.5f, result, 0.001f);
		verify(reglementRepository).getRevenueBetweenDates(start, end);
	}

	@Test
	@DisplayName("getRevenueBetweenDates retourne 0 si le repository renvoie null")
	void getRevenueBetweenDates_shouldReturnZeroWhenNull() {
		Date start = new Date(1_000_000L);
		Date end = new Date(2_000_000L);
		when(reglementRepository.getRevenueBetweenDates(start, end)).thenReturn(null);

		float result = reglementService.getRevenueBetweenDates(start, end);

		assertEquals(0.0f, result, 0.001f);
	}

	@Test
	@DisplayName("getRevenueBetweenDates accepte des dates égales")
	void getRevenueBetweenDates_shouldAcceptEqualDates() {
		Date same = new Date(1_000_000L);
		when(reglementRepository.getRevenueBetweenDates(same, same)).thenReturn(10f);

		float result = reglementService.getRevenueBetweenDates(same, same);

		assertEquals(10f, result, 0.001f);
	}

	@Test
	@DisplayName("getRevenueBetweenDates lève IllegalArgumentException si start > end")
	void getRevenueBetweenDates_shouldThrowWhenStartAfterEnd() {
		Date start = new Date(2_000_000L);
		Date end = new Date(1_000_000L);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> reglementService.getRevenueBetweenDates(start, end));

		assertEquals("Start date must be before or equal to end date", ex.getMessage());
		verifyNoInteractions(reglementRepository);
	}
}