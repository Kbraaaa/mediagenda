package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.EstadoFranja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface FranjaRepositoryJPA extends JpaRepository<FranjaEntity, Long>, JpaSpecificationExecutor<FranjaEntity> {

    /** Dos rangos se cruzan si uno empieza antes de que el otro termine y viceversa. */
    @Query("select count(f) > 0 from FranjaEntity f where f.medico.idMedico = :idMedico "
            + "and f.fecha = :fecha and f.horaInicio < :fin and f.horaFin > :inicio")
    boolean haySolapamiento(@Param("idMedico") UUID idMedico, @Param("fecha") LocalDate fecha,
                            @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin);

    /**
     * Cambia el estado solo si la franja esta en el estado esperado. Es UNA sentencia UPDATE:
     * si dos pacientes reservan a la vez, PostgreSQL bloquea la fila y solo una actualizacion gana.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FranjaEntity f set f.estado = :nuevo where f.idFranja = :id and f.estado = :actual")
    int cambiarEstado(@Param("id") Long id, @Param("actual") EstadoFranja actual, @Param("nuevo") EstadoFranja nuevo);
}
