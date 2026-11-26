package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.CompatibilidadSanguinea;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.exception.ReglaNegocioException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/** Indica si un paciente (donante) puede donar globulos rojos a otro (receptor) segun sus tipos de sangre. */
@RequiredArgsConstructor
public class CompatibilidadSanguineaUseCase {

    private final PacienteGateway pacienteGateway;

    public CompatibilidadSanguinea ejecutar(String codigoReceptor, String codigoDonante) {
        if (codigoDonante == null || codigoDonante.isBlank()) {
            throw new IllegalArgumentException("El codigo del donante es obligatorio");
        }
        if (codigoReceptor.equalsIgnoreCase(codigoDonante.trim())) {
            throw new IllegalArgumentException("El donante y el receptor deben ser pacientes distintos");
        }
        Paciente receptor = buscarConTipo(codigoReceptor);
        Paciente donante = buscarConTipo(codigoDonante.trim());
        return CompatibilidadSanguinea.evaluar(receptor.getCodigo(), receptor.getTipoSangre(),
                donante.getCodigo(), donante.getTipoSangre());
    }

    private Paciente buscarConTipo(String codigo) {
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (paciente.getTipoSangre() == null || paciente.getTipoSangre().isBlank()) {
            throw new ReglaNegocioException("TIPO_SANGRE_NO_REGISTRADO",
                    "El paciente " + codigo + " no tiene tipo de sangre registrado");
        }
        return paciente;
    }
}
