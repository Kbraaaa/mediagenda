package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import lombok.RequiredArgsConstructor;

/** Mueve una cita AGENDADA a otra franja: reserva la nueva primero y solo despues libera la anterior. */
@RequiredArgsConstructor
public class ReprogramarCitaUseCase {

    private final CitaGateway citaGateway;
    private final FranjaGateway franjaGateway;
    private final EventoGateway eventoGateway;
    private final ReglasAgenda reglas;

    public Cita ejecutar(String codigo, Long idNuevaFranja) {
        if (idNuevaFranja == null) {
            throw new IllegalArgumentException("El campo idFranja es obligatorio");
        }
        Cita cita = ReglasAgenda.buscarAgendada(citaGateway, codigo);
        Long idAnterior = cita.getFranja().getIdFranja();
        if (idNuevaFranja.equals(idAnterior)) {
            throw new IllegalArgumentException("La cita ya esta en la franja " + idNuevaFranja);
        }
        Franja nueva = franjaGateway.buscarPorId(idNuevaFranja)
                .orElseThrow(() -> new NoEncontradoException("FRANJA_NO_ENCONTRADA",
                        "No existe la franja " + idNuevaFranja));
        reglas.validarFranjaReservable(nueva);
        if (!franjaGateway.reservar(idNuevaFranja)) {
            throw new ConflictoException("FRANJA_OCUPADA", "La franja " + idNuevaFranja + " ya esta ocupada");
        }
        nueva.setEstado(EstadoFranja.RESERVADA);
        cita.setFranja(nueva);
        cita.setRecordatorioEnviado(false);

        Cita guardada;
        try {
            guardada = citaGateway.guardar(cita);
        } catch (RuntimeException e) {
            franjaGateway.liberar(idNuevaFranja);
            throw e;
        }
        franjaGateway.liberar(idAnterior);
        eventoGateway.publicar(EventoCita.de(EventoCita.REPROGRAMADA, guardada, "Cita reprogramada"));
        return guardada;
    }
}
