package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Especialidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MedicoRepositoryJPA extends JpaRepository<MedicoEntity, UUID> {

    Optional<MedicoEntity> findByCodigo(String codigo);

    boolean existsByCorreoIgnoreCase(String correo);

    Page<MedicoEntity> findByEspecialidad(Especialidad especialidad, Pageable pageable);

    @Query(value = "SELECT nextval('medico_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
