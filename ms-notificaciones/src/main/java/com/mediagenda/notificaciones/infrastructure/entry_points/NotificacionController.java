package com.mediagenda.notificaciones.infrastructure.entry_points;

import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.usecase.ConsultarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.CrearNotificacionManualUseCase;
import com.mediagenda.notificaciones.domain.usecase.ListarNotificacionesUseCase;
import com.mediagenda.notificaciones.domain.usecase.MarcarLeidaUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarNotificacionUseCase;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.NotificacionPeticion;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.NotificacionRespuesta;
import com.mediagenda.notificaciones.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.notificaciones.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Las 5 APIs de ms-notificaciones. Los avisos automaticos llegan por RabbitMQ, no por aqui. */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final CrearNotificacionManualUseCase crear;
    private final ConsultarNotificacionUseCase consultar;
    private final ListarNotificacionesUseCase listar;
    private final ReintentarNotificacionUseCase reintentar;
    private final MarcarLeidaUseCase marcarLeida;
    private final DtoMapper mapper;

    /** 1. Enviar aviso manual: 202 (se envia en segundo plano), 400. */
    @PostMapping
    public ResponseEntity<NotificacionRespuesta> enviar(@RequestBody NotificacionPeticion p) {
        if (p == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        var n = crear.ejecutar(p.destinatario(), p.asunto(), p.cuerpo(), p.codigoPaciente());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRespuesta(n));
    }

    /** 2. Consultar estado de un aviso: 200, 404. */
    @GetMapping("/{codigo}")
    public ResponseEntity<NotificacionRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultar.ejecutar(codigo)));
    }

    /** 3. Historial paginado por paciente y estado: 200, 400. */
    @GetMapping
    public ResponseEntity<PaginaRespuesta<NotificacionRespuesta>> listar(
            @RequestParam(required = false) String paciente,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        FiltroNotificaciones filtro = new FiltroNotificaciones(DtoMapper.textoOpcional(paciente),
                DtoMapper.estadoOpcional(estado));
        return ResponseEntity.ok(mapper.toRespuesta(listar.ejecutar(filtro, page, size)));
    }

    /** 4. Reintentar un aviso FALLIDA: 202, 404, 409. */
    @PostMapping("/{codigo}/reintento")
    public ResponseEntity<NotificacionRespuesta> reintentar(@PathVariable String codigo) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRespuesta(reintentar.ejecutar(codigo)));
    }

    /** 5. Marcar como leida: 200, 404, 409. */
    @PatchMapping("/{codigo}/lectura")
    public ResponseEntity<NotificacionRespuesta> marcarLeida(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(marcarLeida.ejecutar(codigo)));
    }
}
