package com.mediagenda.farmacia.domain.model;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Evento hacia ms-notificaciones. Comparte con los eventos de citas los campos comunes
 * (eventoId, tipo, codigoPaciente, nombrePaciente, correoPaciente, telefonoPaciente, detalle).
 */
public record EventoFormula(
        String eventoId,
        String tipo,
        String codigoFormula,
        String codigoCita,
        String codigoPaciente,
        String nombrePaciente,
        String correoPaciente,
        String telefonoPaciente,
        String medicamentos,
        String detalle) {

    public static final String EMITIDA = "formula.emitida";
    public static final String DISPENSADA = "formula.dispensada";

    public static EventoFormula de(String tipo, Formula f, String detalle) {
        String medicamentos = f.getItems().stream()
                .map(i -> i.cantidad() + " x " + i.medicamento().getNombre() + " " + i.medicamento().getPresentacion())
                .collect(Collectors.joining("; "));
        return new EventoFormula(UUID.randomUUID().toString(), tipo, f.getCodigo(), f.getCodigoCita(),
                f.getCodigoPaciente(), f.getNombrePaciente(), f.getCorreoPaciente(), f.getTelefonoPaciente(),
                medicamentos, detalle);
    }
}
