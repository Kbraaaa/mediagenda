package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CitaRepositoryJPA extends JpaRepository<CitaEntity, UUID>, JpaSpecificationExecutor<CitaEntity> {

    Optional<CitaEntity> findByCodigo(String codigo);

    @Query("select c from CitaEntity c where c.estado = :estado and c.recordatorioEnviado = false "
            + "and (c.franja.fecha > :desdeFecha or (c.franja.fecha = :desdeFecha and c.franja.horaInicio >= :desdeHora)) "
            + "and (c.franja.fecha < :hastaFecha or (c.franja.fecha = :hastaFecha and c.franja.horaInicio <= :hastaHora))")
    List<CitaEntity> pendientesDeRecordatorio(@Param("estado") EstadoCita estado,
                                              @Param("desdeFecha") LocalDate desdeFecha,
                                              @Param("desdeHora") LocalTime desdeHora,
                                              @Param("hastaFecha") LocalDate hastaFecha,
                                              @Param("hastaHora") LocalTime hastaHora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CitaEntity c set c.estadoTriage = :estado, c.especialidadSugerida = :especialidad, "
            + "c.prioridad = :prioridad, c.motivoTriage = :motivo where c.codigo = :codigo")
    int completarTriage(@Param("codigo") String codigo, @Param("estado") EstadoTriage estado,
                        @Param("especialidad") Especialidad especialidad, @Param("prioridad") Prioridad prioridad,
                        @Param("motivo") String motivo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CitaEntity c set c.estadoTriage = :estado where c.codigo = :codigo")
    int cambiarEstadoTriage(@Param("codigo") String codigo, @Param("estado") EstadoTriage estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CitaEntity c set c.recordatorioEnviado = true where c.codigo = :codigo")
    int marcarRecordatorioEnviado(@Param("codigo") String codigo);

    @Query(value = "SELECT nextval('cita_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
