package com.mediagenda.farmacia.domain.model;

/** Lo que ms-farmacia necesita saber de una cita (lo entrega ms-citas por REST). */
public record DatosCita(String codigo, String codigoPaciente, String nombrePaciente, String correoPaciente,
                        String telefonoPaciente, String estado) {

    public boolean atendida() {
        return "ATENDIDA".equals(estado);
    }
}
