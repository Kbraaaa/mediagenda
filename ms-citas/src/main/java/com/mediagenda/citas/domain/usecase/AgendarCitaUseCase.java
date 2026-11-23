package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Codigos;
import com.mediagenda.citas.domain.model.DatosPaciente;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Agenda una cita. Orden: validar todo sin modificar nada, reservar la franja con un UPDATE atomico
 * (si otro paciente la tomo primero, 409), guardar la cita, publicar el evento y pedir el triage
 * en segundo plano. Si guardar falla despues de reservar, se libera la franja (compensacion).
 */
@RequiredArgsConstructor
public class AgendarCitaUseCase {

    private final PacienteGateway pacienteGateway;
    private final FranjaGateway franjaGateway;
    private final CitaGateway citaGateway;
    private final EventoGateway eventoGateway;
    private final SolicitudTriageGateway solicitudTriage;
    private final ReglasAgenda reglas;

    public Cita ejecutar(String codigoPaciente, Long idFranja, String sintomas) {
        ReglasAgenda.validarTexto(codigoPaciente, "codigoPaciente", 1, 20);
        if (idFranja == null) {
            throw new IllegalArgumentException("El campo idFranja es obligatorio");
        }
        ReglasAgenda.validarTexto(sintomas, "sintomas", 10, 1000);

        DatosPaciente paciente = pacienteGateway.buscar(codigoPaciente.trim())
                .orElseThrow(() -> new NoEncontradoException("PACIENTE_NO_ENCONTRADO",
                        "No existe el paciente " + codigoPaciente));
        if (!paciente.activo()) {
            throw new ConflictoException("PACIENTE_INACTIVO", "El paciente " + codigoPaciente + " esta inactivo");
        }
        Franja franja = franjaGateway.buscarPorId(idFranja)
                .orElseThrow(() -> new NoEncontradoException("FRANJA_NO_ENCONTRADA", "No existe la franja " + idFranja));
        reglas.validarFranjaReservable(franja);

        if (!franjaGateway.reservar(idFranja)) {
            throw new ConflictoException("FRANJA_OCUPADA",
                    "La franja " + idFranja + " ya fue tomada por otro paciente");
        }
        franja.setEstado(EstadoFranja.RESERVADA);

        Cita cita = Cita.builder()
                .idCita(UUID.randomUUID())
                .codigoPaciente(paciente.codigo())
                .nombrePaciente(paciente.nombreCompleto())
                .correoPaciente(paciente.correo())
                .telefonoPaciente(paciente.telefono())
                .franja(franja)
                .sintomas(sintomas.trim())
                .estadoTriage(EstadoTriage.PENDIENTE)
                .estado(EstadoCita.AGENDADA)
                .fechaCreacion(reglas.ahora())
                .build();

        Cita guardada;
        try {
            cita.setCodigo(Codigos.cita(reglas.hoy().getYear(), citaGateway.siguienteConsecutivo()));
            guardada = citaGateway.guardar(cita);
        } catch (RuntimeException e) {
            franjaGateway.liberar(idFranja);
            throw e;
        }

        eventoGateway.publicar(EventoCita.de(EventoCita.AGENDADA, guardada, "Cita agendada"));
        solicitudTriage.solicitar(guardada.getCodigo());
        return guardada;
    }
}
