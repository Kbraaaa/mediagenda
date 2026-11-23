package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FormulaRepositoryJPA extends JpaRepository<FormulaEntity, UUID>, JpaSpecificationExecutor<FormulaEntity> {

    Optional<FormulaEntity> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FormulaEntity f where f.codigo = :codigo")
    Optional<FormulaEntity> bloquearPorCodigo(@Param("codigo") String codigo);

    boolean existsByCodigoCitaAndEstadoNot(String codigoCita, EstadoFormula estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormulaEntity f set f.estado = :estado, f.fechaDispensacion = :fecha where f.codigo = :codigo")
    int actualizarEstado(@Param("codigo") String codigo, @Param("estado") EstadoFormula estado,
                         @Param("fecha") LocalDateTime fecha);

    @Query(value = "SELECT nextval('formula_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
