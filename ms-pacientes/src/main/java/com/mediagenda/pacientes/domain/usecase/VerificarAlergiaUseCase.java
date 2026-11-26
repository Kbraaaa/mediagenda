package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Alergias;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.VerificacionAlergia;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Antes de formular, revisa si la sustancia coincide con alguna alergia registrada del paciente:
 * la misma sustancia o un medicamento de la misma familia (por ejemplo, penicilina y amoxicilina).
 */
@RequiredArgsConstructor
public class VerificarAlergiaUseCase {

    private final PacienteGateway pacienteGateway;

    public VerificacionAlergia ejecutar(String codigo, String sustancia) {
        Textos.validar(sustancia, "sustancia", 3, 60);
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        List<String> coincidencias = Alergias.separar(paciente.getAlergias()).stream()
                .map(a -> Alergias.coincidencia(a, sustancia))
                .flatMap(java.util.Optional::stream)
                .toList();
        return new VerificacionAlergia(paciente.getCodigo(), sustancia.trim(), !coincidencias.isEmpty(), coincidencias);
    }
}
