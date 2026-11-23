package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.CodigoPaciente;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.ValidadorPaciente;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Registra un paciente nuevo: valida, revisa duplicados y le asigna su codigo PAC-xxxxxx. */
@RequiredArgsConstructor
public class RegistrarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public Paciente ejecutar(Paciente datos) {
        ValidadorPaciente.validar(datos);
        Paciente paciente = Normalizador.normalizar(datos);

        if (pacienteGateway.existeDocumento(paciente.getTipoDocumento(), paciente.getNumeroDocumento())) {
            throw new PacienteDuplicadoException("Ya existe un paciente con el documento "
                    + paciente.getTipoDocumento() + " " + paciente.getNumeroDocumento());
        }
        if (pacienteGateway.existeCorreo(paciente.getCorreo())) {
            throw new PacienteDuplicadoException("Ya existe un paciente con el correo " + paciente.getCorreo());
        }

        paciente.setIdPaciente(UUID.randomUUID());
        paciente.setCodigo(CodigoPaciente.formatear(pacienteGateway.siguienteConsecutivo()));
        paciente.setActivo(true);
        paciente.setFechaRegistro(LocalDateTime.now());
        return pacienteGateway.guardar(paciente);
    }
}
