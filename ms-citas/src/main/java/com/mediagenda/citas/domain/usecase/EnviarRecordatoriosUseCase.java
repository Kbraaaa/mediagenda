package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Tarea programada: publica un recordatorio para cada cita que empieza en las proximas 24 horas. */
@RequiredArgsConstructor
public class EnviarRecordatoriosUseCase {

    private final CitaGateway citaGateway;
    private final EventoGateway eventoGateway;
    private final ReglasAgenda reglas;

    public int ejecutar() {
        LocalDateTime ahora = reglas.ahora();
        List<Cita> citas = citaGateway.pendientesDeRecordatorio(ahora, ahora.plusHours(24));
        for (Cita cita : citas) {
            eventoGateway.publicar(EventoCita.de(EventoCita.RECORDATORIO, cita, "Su cita es en menos de 24 horas"));
            citaGateway.marcarRecordatorioEnviado(cita.getCodigo());
        }
        return citas.size();
    }
}
