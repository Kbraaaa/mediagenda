package com.mediagenda.pacientes.infrastructure.jpa_driver_adapter;

import com.mediagenda.pacientes.domain.model.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PacienteRepositoryJPA extends JpaRepository<PacienteEntity, UUID> {

    Optional<PacienteEntity> findByCodigo(String codigo);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndCodigoNot(String correo, String codigo);

    Page<PacienteEntity> findByActivoTrue(Pageable pageable);

    @Query("select p from PacienteEntity p where p.activo = true "
            + "and lower(concat(p.nombres, ' ', p.apellidos)) like :patron")
    Page<PacienteEntity> buscarActivosPorNombre(@Param("patron") String patron, Pageable pageable);

    @Query(value = "SELECT nextval('paciente_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
