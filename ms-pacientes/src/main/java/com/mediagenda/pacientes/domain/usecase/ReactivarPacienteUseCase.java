package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/** Vuelve a activar a un paciente desactivado (por ejemplo, regresa a la IPS) conservando su codigo e historial. */
@RequiredArgsConstructor
public class ReactivarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public Paciente ejecutar(String codigo) {
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (paciente.isActivo()) {
            throw new ConflictoPacienteException("PACIENTE_YA_ACTIVO", "El paciente " + codigo + " ya esta activo");
        }
        paciente.setActivo(true);
        return pacienteGateway.guardar(paciente);
    }
}
