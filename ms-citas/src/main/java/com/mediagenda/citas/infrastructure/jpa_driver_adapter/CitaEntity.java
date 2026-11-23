package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.Prioridad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
public class CitaEntity {

    @Id
    @Column(name = "id_cita")
    private UUID idCita;

    @Column(nullable = false, unique = true, length = 16)
    private String codigo;

    @Column(name = "codigo_paciente", nullable = false, length = 20)
    private String codigoPaciente;

    @Column(name = "nombre_paciente", nullable = false, length = 170)
    private String nombrePaciente;

    @Column(name = "correo_paciente", nullable = false, length = 120)
    private String correoPaciente;

    @Column(name = "telefono_paciente", length = 20)
    private String telefonoPaciente;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_franja", nullable = false)
    private FranjaEntity franja;

    @Column(nullable = false, length = 1000)
    private String sintomas;

    @Enumerated(EnumType.STRING)
    @Column(name = "especialidad_sugerida", length = 30)
    private Especialidad especialidadSugerida;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Prioridad prioridad;

    @Column(name = "motivo_triage", length = 500)
    private String motivoTriage;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_triage", nullable = false, length = 12)
    private EstadoTriage estadoTriage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCita estado;

    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
