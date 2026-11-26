package com.mediagenda.notificaciones.infrastructure.entry_points;

import com.mediagenda.notificaciones.domain.usecase.CorregirDestinatarioUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReenviarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarFallidasUseCase;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.DestinatarioPeticion;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.NotificacionRespuesta;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.ReintentosRespuesta;
import com.mediagenda.notificaciones.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de soporte sobre los avisos enviados, distintas del CRUD. */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class OperacionesNotificacionController {

    private final ReenviarNotificacionUseCase reenviar;
    private final CorregirDestinatarioUseCase corregirDestinatario;
    private final ReintentarFallidasUseCase reintentarFallidas;
    private final DtoMapper mapper;

    /** 202: la copia queda creada y el envio corre en segundo plano. */
    @PostMapping("/{codigo}/reenvio")
    public ResponseEntity<NotificacionRespuesta> reenviar(@PathVariable String codigo) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRespuesta(reenviar.ejecutar(codigo)));
    }

    @PatchMapping("/{codigo}/destinatario")
    public ResponseEntity<NotificacionRespuesta> corregirDestinatario(@PathVariable String codigo,
                                                                      @RequestBody DestinatarioPeticion peticion) {
        String destinatario = peticion == null ? null : peticion.destinatario();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toRespuesta(corregirDestinatario.ejecutar(codigo, destinatario)));
    }

    /** Reintenta ya, en lote, los avisos fallidos que no agotaron sus intentos (lo mismo que la tarea programada). */
    @PostMapping("/reintentos")
    public ResponseEntity<ReintentosRespuesta> reintentarFallidas() {
        return ResponseEntity.ok(new ReintentosRespuesta(reintentarFallidas.ejecutar()));
    }
}
