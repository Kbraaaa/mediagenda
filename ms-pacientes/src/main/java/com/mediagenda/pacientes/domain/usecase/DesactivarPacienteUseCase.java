package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/**
 * Borrado logico: el paciente queda inactivo pero su historial se conserva,
 * porque las citas y formulas de otros servicios lo siguen referenciando.
 * Desactivar dos veces no es error (operacion idempotente).
 */
@RequiredArgsConstructor
public class DesactivarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public void ejecutar(String codigo) {
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (paciente.isActivo()) {
            paciente.setActivo(false);
            pacienteGateway.guardar(paciente);
        }
    }
}
