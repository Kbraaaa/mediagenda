package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Telefonos;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

/**
 * Corrige el correo o el celular de un aviso que fallo (por ejemplo, mal escrito) y lo vuelve a enviar
 * desde cero: intentos en 0, sin el ultimo error y en estado PENDIENTE.
 */
@RequiredArgsConstructor
public class CorregirDestinatarioUseCase {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificacionGateway notificaciones;
    private final SolicitudEnvioGateway solicitudEnvio;

    public Notificacion ejecutar(String codigo, String destinatario) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }
        Notificacion n = new ConsultarNotificacionUseCase(notificaciones).ejecutar(codigo);
        if (n.getEstado() != EstadoNotificacion.FALLIDA) {
            throw new ConflictoException("NOTIFICACION_NO_FALLIDA",
                    "Solo se corrige el destinatario de notificaciones FALLIDA; esta esta " + n.getEstado());
        }
        n.setDestinatario(validar(n.getCanal(), destinatario.trim()));
        n.setIntentos(0);
        n.setUltimoError(null);
        n.setEstado(EstadoNotificacion.PENDIENTE);
        Notificacion guardada = notificaciones.guardar(n);
        solicitudEnvio.solicitar(codigo);
        return guardada;
    }

    private static String validar(Canal canal, String destinatario) {
        if (canal == Canal.SMS) {
            return Telefonos.aE164(destinatario).orElseThrow(() ->
                    new IllegalArgumentException("El destinatario debe ser un celular valido, por ejemplo 3101234567"));
        }
        if (destinatario.length() > 120 || !CORREO.matcher(destinatario).matches()) {
            throw new IllegalArgumentException("El destinatario debe ser un correo valido");
        }
        return destinatario;
    }
}
