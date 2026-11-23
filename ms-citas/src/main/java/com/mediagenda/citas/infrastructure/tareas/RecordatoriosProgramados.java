package com.mediagenda.citas.infrastructure.tareas;

import com.mediagenda.citas.domain.usecase.EnviarRecordatoriosUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada 5 minutos busca citas de las proximas 24 horas y publica su recordatorio. */
@Component
@RequiredArgsConstructor
public class RecordatoriosProgramados {

    private static final Logger log = LoggerFactory.getLogger(RecordatoriosProgramados.class);

    private final EnviarRecordatoriosUseCase enviarRecordatorios;

    @Scheduled(fixedDelayString = "${citas.recordatorios.intervalo-ms:300000}", initialDelay = 60000)
    public void ejecutar() {
        try {
            int enviados = enviarRecordatorios.ejecutar();
            if (enviados > 0) {
                log.info("Recordatorios publicados: {}", enviados);
            }
        } catch (RuntimeException e) {
            log.error("Fallo el envio de recordatorios: {}", e.getMessage());
        }
    }
}
