package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Especialidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@NoArgsConstructor
public class MedicoEntity {

    @Id
    @Column(name = "id_medico")
    private UUID idMedico;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Especialidad especialidad;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(nullable = false)
    private boolean activo;
}
