package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import lombok.RequiredArgsConstructor;

/**
 * Vuelve a pedir el triage de una cita cuando el analisis con IA fallo (Gemini no respondio tras sus reintentos).
 * El analisis corre en segundo plano, igual que al agendar: la respuesta sale con el triage PENDIENTE.
 */
@RequiredArgsConstructor
public class ReintentarTriageUseCase {

    private final CitaGateway citaGateway;
    private final SolicitudTriageGateway solicitudTriage;

    public Cita ejecutar(String codigo) {
        Cita cita = ReglasAgenda.buscarAgendada(citaGateway, codigo);
        if (cita.getEstadoTriage() != EstadoTriage.FALLIDO) {
            throw new ConflictoException("TRIAGE_NO_FALLIDO", "El triage de la cita " + codigo + " esta "
                    + cita.getEstadoTriage() + ": solo se vuelve a pedir cuando FALLIDO");
        }
        citaGateway.actualizarTriage(codigo, EstadoTriage.PENDIENTE, null);
        cita.setEstadoTriage(EstadoTriage.PENDIENTE);
        solicitudTriage.solicitar(codigo);
        return cita;
    }
}
