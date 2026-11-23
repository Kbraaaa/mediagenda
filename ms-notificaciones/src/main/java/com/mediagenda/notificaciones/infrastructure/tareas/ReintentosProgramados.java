package com.mediagenda.notificaciones.infrastructure.tareas;

import com.mediagenda.notificaciones.domain.usecase.ReintentarFallidasUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada 5 minutos reintenta los correos que fallaron (hasta 3 intentos por aviso). */
@Component
@RequiredArgsConstructor
public class ReintentosProgramados {

    private static final Logger log = LoggerFactory.getLogger(ReintentosProgramados.class);

    private final ReintentarFallidasUseCase reintentarFallidas;

    @Scheduled(fixedDelayString = "${notificaciones.reintentos.intervalo-ms:300000}", initialDelay = 60000)
    public void ejecutar() {
        try {
            int reintentadas = reintentarFallidas.ejecutar();
            if (reintentadas > 0) {
                log.info("Notificaciones reintentadas: {}", reintentadas);
            }
        } catch (RuntimeException e) {
            log.error("Fallo el reintento automatico: {}", e.getMessage());
        }
    }
}
