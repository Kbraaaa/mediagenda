package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoRepositoryJPA extends JpaRepository<MedicamentoEntity, UUID> {

    Optional<MedicamentoEntity> findByCodigo(String codigo);

    Page<MedicamentoEntity> findByActivoTrue(Pageable pageable);

    Page<MedicamentoEntity> findByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

    boolean existsByNombreIgnoreCaseAndPresentacionIgnoreCase(String nombre, String presentacion);

    /** SELECT ... FOR UPDATE, en orden de id para que dos transacciones no se bloqueen mutuamente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MedicamentoEntity m where m.idMedicamento in :ids order by m.idMedicamento")
    List<MedicamentoEntity> bloquear(@Param("ids") List<UUID> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MedicamentoEntity m set m.stock = :stock where m.idMedicamento = :id")
    int actualizarStock(@Param("id") UUID id, @Param("stock") int stock);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MedicamentoEntity m set m.stock = m.stock + :cantidad where m.idMedicamento = :id")
    int sumarStock(@Param("id") UUID id, @Param("cantidad") int cantidad);

    @Query(value = "SELECT nextval('medicamento_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
