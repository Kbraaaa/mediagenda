package com.mediagenda.notificaciones.infrastructure.eventos;

import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * En AWS: lee la cola Amazon SQS con "long polling" (espera hasta 20 s a que llegue algo, sin gastar
 * peticiones vacias). Un mensaje solo se borra si se proceso bien. Si falla, SQS lo vuelve a entregar
 * y, tras varios intentos, lo pasa sola a la cola de mensajes muertos (politica de redrive de la cola).
 * Procesar dos veces no duplica correos: ProcesarEvento es idempotente por eventoId.
 */
public class SqsEventoConsumidor {

    private static final Logger log = LoggerFactory.getLogger(SqsEventoConsumidor.class);

    private final SqsClient sqs;
    private final String colaUrl;
    private final ProcesarEventoUseCase procesarEvento;

    public SqsEventoConsumidor(SqsClient sqs, String colaUrl, ProcesarEventoUseCase procesarEvento) {
        this.sqs = sqs;
        this.colaUrl = colaUrl;
        this.procesarEvento = procesarEvento;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.pausa-ms:1000}")
    public int sondear() {
        List<Message> mensajes;
        try {
            mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(colaUrl).maxNumberOfMessages(10).waitTimeSeconds(20).build()).messages();
        } catch (RuntimeException e) {
            log.error("No se pudo leer la cola SQS: {}", e.getMessage());
            return 0;
        }
        int procesados = 0;
        for (Message mensaje : mensajes) {
            try {
                procesarEvento.ejecutar(LectorEventos.leer(mensaje.body()));
                sqs.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(colaUrl).receiptHandle(mensaje.receiptHandle()).build());
                procesados++;
            } catch (RuntimeException e) {
                log.error("Evento {} no procesado; SQS lo reintentara y luego ira a la cola de muertos: {}",
                        mensaje.messageId(), e.getMessage());
            }
        }
        return procesados;
    }
}
