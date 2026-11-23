package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import lombok.RequiredArgsConstructor;

/** Cancela una cita AGENDADA y deja su franja libre para otro paciente. */
@RequiredArgsConstructor
public class CancelarCitaUseCase {

    private final CitaGateway citaGateway;
    private final FranjaGateway franjaGateway;
    private final EventoGateway eventoGateway;

    public Cita ejecutar(String codigo) {
        Cita cita = ReglasAgenda.buscarAgendada(citaGateway, codigo);
        cita.setEstado(EstadoCita.CANCELADA);
        Cita guardada = citaGateway.guardar(cita);
        franjaGateway.liberar(cita.getFranja().getIdFranja());
        guardada.getFranja().setEstado(EstadoFranja.DISPONIBLE);
        eventoGateway.publicar(EventoCita.de(EventoCita.CANCELADA, guardada, "Cita cancelada"));
        return guardada;
    }
}
