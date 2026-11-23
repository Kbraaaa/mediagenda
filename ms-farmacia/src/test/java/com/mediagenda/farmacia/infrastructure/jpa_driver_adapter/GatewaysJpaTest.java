package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.infrastructure.mapper.EntidadesMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewaysJpaTest {

    private final EntidadesMapper mapper = new EntidadesMapper();
    private MedicamentoRepositoryJPA medicamentoRepo;
    private FormulaRepositoryJPA formulaRepo;
    private MedicamentoGatewayImpl medicamentos;
    private FormulaGatewayImpl formulas;

    @BeforeEach
    void preparar() {
        medicamentoRepo = mock(MedicamentoRepositoryJPA.class);
        formulaRepo = mock(FormulaRepositoryJPA.class);
        medicamentos = new MedicamentoGatewayImpl(medicamentoRepo, mapper);
        formulas = new FormulaGatewayImpl(formulaRepo, medicamentoRepo, mapper);
        when(medicamentoRepo.getReferenceById(any())).thenAnswer(i -> {
            UUID id = i.getArgument(0);
            return mapper.toEntity(id.equals(Datos.ID_ACETAMINOFEN) ? Datos.acetaminofen() : Datos.ibuprofeno());
        });
    }

    private FormulaEntity formulaEntity() {
        return mapper.toEntity(Datos.formula(), medicamentoRepo::getReferenceById);
    }

    @Test
    void formulaIdaYVueltaConservaLosItems() {
        Formula original = Datos.formula();

        Formula copia = mapper.toDomain(formulaEntity());

        assertThat(copia).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void medicamentos() {
        when(medicamentoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(medicamentoRepo.findByCodigo("MDT-0001")).thenReturn(Optional.of(mapper.toEntity(Datos.acetaminofen())));
        when(medicamentoRepo.findByActivoTrue(any()))
                .thenReturn(new PageImpl<>(List.of(mapper.toEntity(Datos.acetaminofen())), PageRequest.of(0, 20), 1));
        when(medicamentoRepo.findByActivoTrueAndNombreContainingIgnoreCase(eq("ibu"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(medicamentoRepo.existsByNombreIgnoreCaseAndPresentacionIgnoreCase("a", "b")).thenReturn(true);
        when(medicamentoRepo.bloquear(List.of(Datos.ID_ACETAMINOFEN)))
                .thenReturn(List.of(mapper.toEntity(Datos.acetaminofen())));
        when(medicamentoRepo.siguienteConsecutivo()).thenReturn(9L);

        assertThat(medicamentos.guardar(Datos.acetaminofen()).getCodigo()).isEqualTo("MDT-0001");
        assertThat(medicamentos.buscarPorCodigo("MDT-0001")).isPresent();
        assertThat(medicamentos.listar(null, 0, 20).contenido()).hasSize(1);
        assertThat(medicamentos.listar("ibu", 0, 20).contenido()).isEmpty();
        assertThat(medicamentos.existe("a", "b")).isTrue();
        assertThat(medicamentos.bloquear(List.of(Datos.ID_ACETAMINOFEN))).hasSize(1);
        assertThat(medicamentos.siguienteConsecutivo()).isEqualTo(9L);
        medicamentos.actualizarStock(Datos.ID_ACETAMINOFEN, 480);
        medicamentos.sumarStock(Datos.ID_ACETAMINOFEN, 10);
        verify(medicamentoRepo).actualizarStock(Datos.ID_ACETAMINOFEN, 480);
        verify(medicamentoRepo).sumarStock(Datos.ID_ACETAMINOFEN, 10);
    }

    @SuppressWarnings("unchecked")
    @Test
    void formulas() {
        // Las entidades se arman ANTES de configurar los simuladores: formulaEntity() usa otro simulador
        FormulaEntity primera = formulaEntity();
        FormulaEntity segunda = formulaEntity();
        FormulaEntity tercera = formulaEntity();
        when(formulaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(formulaRepo.findByCodigo("FOR-2026-000001")).thenReturn(Optional.of(primera));
        when(formulaRepo.bloquearPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(segunda));
        when(formulaRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tercera), PageRequest.of(0, 10), 1));
        when(formulaRepo.existsByCodigoCitaAndEstadoNot("CIT-2026-000001", EstadoFormula.ANULADA)).thenReturn(true);
        when(formulaRepo.siguienteConsecutivo()).thenReturn(5L);

        assertThat(formulas.guardar(Datos.formula()).getItems()).hasSize(2);
        assertThat(formulas.buscarPorCodigo("FOR-2026-000001")).isPresent();
        assertThat(formulas.bloquearPorCodigo("FOR-2026-000001")).isPresent();
        assertThat(formulas.listar(new FiltroFormulas(null, null), 0, 10).totalElementos()).isEqualTo(1);
        assertThat(formulas.existeActivaParaCita("CIT-2026-000001")).isTrue();
        assertThat(formulas.siguienteConsecutivo()).isEqualTo(5L);
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 11, 0);
        formulas.actualizarEstado("FOR-2026-000001", EstadoFormula.DISPENSADA, ahora);
        verify(formulaRepo).actualizarEstado("FOR-2026-000001", EstadoFormula.DISPENSADA, ahora);
    }

    @SuppressWarnings("unchecked")
    @Test
    void especificaciones() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Especificaciones.formulas(new FiltroFormulas("PAC-000001", EstadoFormula.EMITIDA))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), cb);
        verify(cb, times(2)).equal(any(), any(Object.class));
        verify(cb).and(any(Predicate[].class));

        CriteriaBuilder vacio = mock(CriteriaBuilder.class);
        Especificaciones.formulas(new FiltroFormulas(null, null))
                .toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), mock(CriteriaQuery.class), vacio);
        verify(vacio, never()).equal(any(), any(Object.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void transaccionEjecutaElBloque() {
        TransactionTemplate plantilla = mock(TransactionTemplate.class);
        when(plantilla.execute(any())).thenAnswer(i -> ((TransactionCallback<Object>) i.getArgument(0)).doInTransaction(null));

        assertThat(new TransaccionAdapter(plantilla).ejecutar(() -> "hecho")).isEqualTo("hecho");
    }
}
