package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.ValidadorPaciente;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/**
 * Actualiza los datos de contacto y clinicos de un paciente activo.
 * El documento de identidad no se puede cambiar: identifica a la persona.
 */
@RequiredArgsConstructor
public class ActualizarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public Paciente ejecutar(String codigo, Paciente datos) {
        Paciente actual = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (!actual.isActivo()) {
            throw new PacienteInactivoException(codigo);
        }

        ValidadorPaciente.validar(datos);
        Paciente nuevos = Normalizador.normalizar(datos);

        if (nuevos.getTipoDocumento() != actual.getTipoDocumento()
                || !nuevos.getNumeroDocumento().equals(actual.getNumeroDocumento())) {
            throw new IllegalArgumentException("El documento de identidad no se puede modificar");
        }
        if (pacienteGateway.existeCorreoEnOtroPaciente(nuevos.getCorreo(), codigo)) {
            throw new PacienteDuplicadoException("Ya existe otro paciente con el correo " + nuevos.getCorreo());
        }

        Paciente actualizado = actual.toBuilder()
                .nombres(nuevos.getNombres())
                .apellidos(nuevos.getApellidos())
                .correo(nuevos.getCorreo())
                .telefono(nuevos.getTelefono())
                .fechaNacimiento(nuevos.getFechaNacimiento())
                .tipoSangre(nuevos.getTipoSangre())
                .alergias(nuevos.getAlergias())
                .build();
        return pacienteGateway.guardar(actualizado);
    }
}
