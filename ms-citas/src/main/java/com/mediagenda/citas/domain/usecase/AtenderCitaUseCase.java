package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import lombok.RequiredArgsConstructor;

/** El medico marca la cita como atendida. ms-farmacia solo formula sobre citas ATENDIDA. */
@RequiredArgsConstructor
public class AtenderCitaUseCase {

    private final CitaGateway citaGateway;

    public Cita ejecutar(String codigo) {
        Cita cita = ReglasAgenda.buscarAgendada(citaGateway, codigo);
        cita.setEstado(EstadoCita.ATENDIDA);
        return citaGateway.guardar(cita);
    }
}
