package com.mediagenda.citas.infrastructure.tareas;

import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import com.mediagenda.citas.domain.usecase.ProcesarTriageUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/**
 * Ejecuta el triage en un hilo aparte (pool "triageExecutor"), despues de que la API ya respondio.
 * Asi POST /citas responde rapido aunque la IA tarde varios segundos.
 */
public class SolicitudTriageAsincrona implements SolicitudTriageGateway {

    private static final Logger log = LoggerFactory.getLogger(SolicitudTriageAsincrona.class);

    private final ProcesarTriageUseCase procesarTriage;

    public SolicitudTriageAsincrona(ProcesarTriageUseCase procesarTriage) {
        this.procesarTriage = procesarTriage;
    }

    @Override
    @Async("triageExecutor")
    public void solicitar(String codigoCita) {
        try {
            procesarTriage.ejecutar(codigoCita);
        } catch (RuntimeException e) {
            log.error("Fallo el triage de la cita {}: {}", codigoCita, e.getMessage());
        }
    }
}
