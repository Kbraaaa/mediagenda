package com.mediagenda.notificaciones.infrastructure.sms;

import com.mediagenda.notificaciones.domain.model.exception.EnvioSmsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** API de bots de Telegram simulada con MockRestServiceServer (sin red ni token real). */
class TelegramAdapterTest {

    private static final String API = "https://api.telegram.org";

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    private TelegramAdapter adaptador() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        return new TelegramAdapter(builder, API, "123:abc", "999");
    }

    @Test
    void enviaAlChatConfiguradoConElNumeroEnmascarado() {
        TelegramAdapter telegram = adaptador();
        servidor.expect(requestTo(API + "/bot123:abc/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("999"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("*********4567")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("MediAgenda: cita agendada")))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":7}}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> telegram.enviar("+573101234567", "MediAgenda: cita agendada")).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    void siTelegramRechazaSeLanzaEnvioSmsExceptionConLaCausa() {
        TelegramAdapter telegram = adaptador();
        servidor.expect(requestTo(API + "/bot123:abc/sendMessage")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"ok\":false,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> telegram.enviar("+573101234567", "x"))
                .isInstanceOf(EnvioSmsException.class).hasMessageContaining("chat not found").hasMessageContaining("400");
    }

    @Test
    void respuestaConOkFalsoOVaciaTambienEsFallo() {
        TelegramAdapter telegram = adaptador();
        servidor.expect(requestTo(API + "/bot123:abc/sendMessage"))
                .andRespond(withSuccess("{\"ok\":false,\"description\":\"Forbidden: bot was blocked\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(API + "/bot123:abc/sendMessage")).andRespond(withSuccess());

        assertThatThrownBy(() -> telegram.enviar("+573101234567", "x")).hasMessageContaining("bot was blocked");
        assertThatThrownBy(() -> telegram.enviar("+573101234567", "x")).hasMessageContaining("respuesta vacia");
    }

    @Test
    void sinConexionSeLanzaEnvioSmsException() {
        TelegramAdapter telegram = adaptador();
        servidor.expect(requestTo(API + "/bot123:abc/sendMessage")).andRespond(withException(new SocketTimeoutException("lento")));

        assertThatThrownBy(() -> telegram.enviar("+573101234567", "x"))
                .isInstanceOf(EnvioSmsException.class).hasMessageContaining("No fue posible comunicarse")
                .hasMessageNotContaining("123:abc");
    }
}
