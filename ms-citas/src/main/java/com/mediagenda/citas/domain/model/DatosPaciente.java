package com.mediagenda.citas.domain.model;

/** Lo que ms-citas necesita saber de un paciente (lo entrega ms-pacientes por REST). */
public record DatosPaciente(String codigo, String nombreCompleto, String correo, String telefono, boolean activo) {
}
