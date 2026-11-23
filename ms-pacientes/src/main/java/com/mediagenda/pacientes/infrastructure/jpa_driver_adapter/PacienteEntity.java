package com.mediagenda.pacientes.infrastructure.jpa_driver_adapter;

import com.mediagenda.pacientes.domain.model.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Tabla pacientes (creada por Flyway en V1__crear_tabla_pacientes.sql). */
@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
public class PacienteEntity {

    @Id
    @Column(name = "id_paciente")
    private UUID idPaciente;

    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 2)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "tipo_sangre", length = 3)
    private String tipoSangre;

    @Column(length = 500)
    private String alergias;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
