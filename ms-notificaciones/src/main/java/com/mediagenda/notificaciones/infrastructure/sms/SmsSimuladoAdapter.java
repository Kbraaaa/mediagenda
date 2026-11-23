package com.mediagenda.notificaciones.infrastructure.sms;

import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * En local no hay proveedor de SMS: el mensaje se escribe en el log (con el numero enmascarado)
 * y el aviso queda ENVIADO, igual que Mailpit hace con los correos.
 */
public class SmsSimuladoAdapter implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(SmsSimuladoAdapter.class);

    @Override
    public void enviar(String telefonoE164, String mensaje) {
        log.info("SMS simulado a {}: {}", enmascarar(telefonoE164), mensaje);
    }

    static String enmascarar(String telefono) {
        return telefono.length() <= 4 ? "****" : "*".repeat(telefono.length() - 4) + telefono.substring(telefono.length() - 4);
    }
}
