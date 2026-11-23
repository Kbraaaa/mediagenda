package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Codigos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/** Aviso escrito a mano (por ejemplo, desde recepcion). Se guarda PENDIENTE y se envia en segundo plano. */
@RequiredArgsConstructor
public class CrearNotificacionManualUseCase {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificacionGateway notificaciones;
    private final SolicitudEnvioGateway solicitudEnvio;
    private final Despachador despachador;

    public Notificacion ejecutar(String destinatario, String asunto, String cuerpo, String codigoPaciente) {
        if (destinatario == null || !CORREO.matcher(destinatario.trim()).matches()) {
            throw new IllegalArgumentException("El destinatario debe ser un correo valido");
        }
        validar(asunto, "asunto", 200);
        validar(cuerpo, "cuerpo", 2000);
        LocalDateTime ahora = despachador.ahora();
        Notificacion n = notificaciones.guardar(Notificacion.builder()
                .idNotificacion(UUID.randomUUID())
                .codigo(Codigos.notificacion(ahora.getYear(), notificaciones.siguienteConsecutivo()))
                .tipo(Notificacion.TIPO_MANUAL)
                .codigoPaciente(codigoPaciente == null || codigoPaciente.isBlank() ? null : codigoPaciente.trim())
                .destinatario(destinatario.trim())
                .asunto(asunto.trim())
                .cuerpo(cuerpo.trim())
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(ahora)
                .build());
        solicitudEnvio.solicitar(n.getCodigo());
        return n;
    }

    private static void validar(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        if (valor.trim().length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " no puede superar " + maximo + " caracteres");
        }
    }
}
