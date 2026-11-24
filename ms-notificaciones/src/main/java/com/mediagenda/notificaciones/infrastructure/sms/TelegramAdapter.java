package com.mediagenda.notificaciones.infrastructure.sms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediagenda.notificaciones.domain.model.exception.EnvioSmsException;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Canal movil real con la API de bots de Telegram (gratuita, sin sandbox). Reemplaza al SMS de
 * Amazon SNS, que la cuenta del plan gratuito no entrega. Para la demo, todos los avisos llegan al
 * chat configurado (TELEGRAM_CHAT_ID) indicando a que celular iban; vincular un chat por paciente
 * queda como trabajo futuro. El token del bot es un secreto: solo vive en el .env.
 */
public class TelegramAdapter implements SmsGateway {

    private final RestClient telegram;
    private final String token;
    private final String chatId;

    public TelegramAdapter(RestClient.Builder builder, String urlBase, String token, String chatId) {
        this.telegram = builder.baseUrl(urlBase).build();
        this.token = token;
        this.chatId = chatId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(boolean ok, String description) {
    }

    @Override
    public void enviar(String telefonoE164, String mensaje) {
        String texto = "📱 Aviso para " + SmsSimuladoAdapter.enmascarar(telefonoE164) + "\n" + mensaje;
        try {
            Respuesta r = telegram.post()
                    // El token va tal cual en la ruta: como variable de plantilla, Spring codificaria
                    // sus ":" como %3A y Telegram no reconoceria el bot (lo detecto la prueba unitaria)
                    .uri("/bot" + token + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", texto))
                    .retrieve()
                    .body(Respuesta.class);
            if (r == null || !r.ok()) {
                throw new EnvioSmsException(sinToken("Telegram no acepto el mensaje: "
                        + (r == null ? "respuesta vacia" : r.description())), null);
            }
        } catch (RestClientResponseException e) {
            // El cuerpo de error de Telegram trae la causa (chat no encontrado, token invalido...)
            throw new EnvioSmsException(sinToken("Telegram rechazo el mensaje (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString()), null);
        } catch (RestClientException e) {
            throw new EnvioSmsException(sinToken("No fue posible comunicarse con Telegram: " + e.getMessage()), null);
        }
    }

    /**
     * Los errores de Spring incluyen la URL completa, que lleva el token del bot. El mensaje se guarda
     * en la notificacion (ultimoError) y se ve por la API, asi que el token se oculta. Tampoco se
     * encadena la excepcion original, para que el token no termine en el log.
     */
    private String sinToken(String mensaje) {
        return mensaje == null ? null : mensaje.replace(token, "***");
    }
}
