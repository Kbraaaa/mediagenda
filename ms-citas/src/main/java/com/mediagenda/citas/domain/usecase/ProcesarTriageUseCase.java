package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.exception.TriageException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Corre en segundo plano despues de agendar: envia los sintomas a la IA, guarda el resultado y
 * publica un evento para avisar al paciente. Si la IA falla tras los reintentos, el triage queda
 * FALLIDO y la cita sigue valida (la IA es un apoyo, no un requisito para la cita).
 */
@RequiredArgsConstructor
public class ProcesarTriageUseCase {

    private final CitaGateway citaGateway;
    private final TriageGateway triageGateway;
    private final EventoGateway eventoGateway;
    private final int intentosMaximos;

    public void ejecutar(String codigoCita) {
        Optional<Cita> encontrada = citaGateway.buscarPorCodigo(codigoCita);
        if (encontrada.isEmpty() || encontrada.get().getEstadoTriage() != EstadoTriage.PENDIENTE) {
            return;
        }
        Cita cita = encontrada.get();

        ResultadoTriage resultado = null;
        for (int intento = 1; intento <= intentosMaximos && resultado == null; intento++) {
            try {
                resultado = triageGateway.analizar(cita.getSintomas());
            } catch (TriageException e) {
                resultado = null;
            }
        }
        if (resultado == null) {
            citaGateway.actualizarTriage(codigoCita, EstadoTriage.FALLIDO, null);
            return;
        }

        citaGateway.actualizarTriage(codigoCita, EstadoTriage.COMPLETADO, resultado);
        cita.setEspecialidadSugerida(resultado.especialidad());
        cita.setPrioridad(resultado.prioridad());
        cita.setMotivoTriage(resultado.motivo());
        cita.setEstadoTriage(EstadoTriage.COMPLETADO);
        eventoGateway.publicar(EventoCita.de(EventoCita.TRIAGE_COMPLETADO, cita, recomendacion(cita, resultado)));
    }

    /** Texto que recibe el paciente. Siempre aclara que es una orientacion y no un diagnostico. */
    static String recomendacion(Cita cita, ResultadoTriage resultado) {
        StringBuilder texto = new StringBuilder();
        if (resultado.prioridad() == Prioridad.ALTA) {
            texto.append("Sus sintomas podrian requerir atencion inmediata: si empeoran, acuda a urgencias. ");
        }
        if (resultado.especialidad() != cita.getFranja().getMedico().getEspecialidad()) {
            texto.append("Se sugiere la especialidad ").append(resultado.especialidad())
                    .append("; puede reprogramar su cita. ");
        } else {
            texto.append("La especialidad de su cita es adecuada para sus sintomas. ");
        }
        texto.append("Orientacion automatica, no es un diagnostico medico.");
        return texto.toString();
    }
}
