package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Alergias;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/** Agrega una alergia nueva a la historia del paciente sin reescribir el resto de sus datos. */
@RequiredArgsConstructor
public class RegistrarAlergiaUseCase {

    static final int MAXIMO_TOTAL = 500;

    private final PacienteGateway pacienteGateway;

    public Paciente ejecutar(String codigo, String alergia) {
        Textos.validar(alergia, "alergia", 3, 60);
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (!paciente.isActivo()) {
            throw new PacienteInactivoException(codigo);
        }
        String nueva = alergia.trim();
        if (Alergias.contiene(paciente.getAlergias(), nueva)) {
            throw new ConflictoPacienteException("ALERGIA_YA_REGISTRADA",
                    "El paciente " + codigo + " ya tiene registrada la alergia " + nueva);
        }
        String actuales = paciente.getAlergias();
        String todas = actuales == null || actuales.isBlank() ? nueva : actuales.trim() + ", " + nueva;
        if (todas.length() > MAXIMO_TOTAL) {
            throw new IllegalArgumentException("Las alergias del paciente no pueden superar " + MAXIMO_TOTAL + " caracteres");
        }
        paciente.setAlergias(todas);
        return pacienteGateway.guardar(paciente);
    }
}
