package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/** Consulta un paciente por su codigo (activo o inactivo). Lo usa tambien ms-citas. */
@RequiredArgsConstructor
public class ConsultarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public Paciente ejecutar(String codigo) {
        return pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
    }
}
