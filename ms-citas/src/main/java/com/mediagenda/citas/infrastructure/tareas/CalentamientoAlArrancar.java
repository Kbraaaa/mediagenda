package com.mediagenda.citas.infrastructure.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Al arrancar, en segundo plano: abre las conexiones externas (SQS, el otro microservicio) y llama
 * varias veces a las consultas propias para que la JVM optimice el codigo. Asi la primera peticion
 * de un usuario no paga el "arranque en frio" (en AWS llegaba a 500-800 ms; ya caliente, ~150 ms).
 * Solo hace lecturas: no crea ni cambia datos.
 */
public class CalentamientoAlArrancar {

    private static final Logger log = LoggerFactory.getLogger(CalentamientoAlArrancar.class);

    private final RestClient propio;
    private final List<String> rutas;
    private final int repeticiones;
    private final List<Runnable> conexiones;

    public CalentamientoAlArrancar(RestClient propio, List<String> rutas, int repeticiones, List<Runnable> conexiones) {
        this.propio = propio;
        this.rutas = rutas;
        this.repeticiones = repeticiones;
        this.conexiones = conexiones;
    }

    @EventListener(ApplicationReadyEvent.class)
    public CompletableFuture<Integer> alArrancar() {
        return CompletableFuture.supplyAsync(this::calentar);
    }

    /** Devuelve cuantas llamadas propias respondieron bien. */
    public int calentar() {
        for (Runnable conexion : conexiones) {
            try {
                conexion.run();
            } catch (RuntimeException e) {
                log.warn("Calentamiento: una conexion externa no respondio: {}", e.getMessage());
            }
        }
        int correctas = 0;
        for (int i = 0; i < repeticiones; i++) {
            for (String ruta : rutas) {
                try {
                    propio.get().uri(ruta).retrieve().toBodilessEntity();
                    correctas++;
                } catch (RuntimeException e) {
                    // Un 404 o un error tambien calienta el manejador de errores: no se registra
                }
            }
        }
        log.info("Calentamiento terminado: {} llamadas propias correctas", correctas);
        return correctas;
    }
}
