package com.mediagenda.citas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Medico de la IPS. Codigo legible: MED-0001. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    private UUID idMedico;
    private String codigo;
    private String nombre;
    private Especialidad especialidad;
    private String correo;
    private boolean activo;
}
