package com.mediagenda.notificaciones.application;

import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import com.mediagenda.notificaciones.domain.usecase.Despachador;
import com.mediagenda.notificaciones.domain.usecase.EnviarPendienteUseCase;
import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigTest {

    @Test
    void casosDeUso() {
        UseCasesConfig u = new UseCasesConfig();
        NotificacionGateway n = mock(NotificacionGateway.class);
        SolicitudEnvioGateway s = mock(SolicitudEnvioGateway.class);
        Despachador d = u.despachador(mock(CorreoGateway.class), mock(SmsGateway.class), n, Clock.systemUTC());

        assertThat(u.procesarEventoUseCase(n, d, "simulado")).isNotNull();
        assertThat(u.procesarEventoUseCase(n, d, "ninguno")).isNotNull();
        assertThat(u.crearNotificacionManualUseCase(n, s, d)).isNotNull();
        assertThat(u.consultarNotificacionUseCase(n)).isNotNull();
        assertThat(u.listarNotificacionesUseCase(n)).isNotNull();
        assertThat(u.reintentarNotificacionUseCase(n, s)).isNotNull();
        assertThat(u.marcarLeidaUseCase(n)).isNotNull();
        assertThat(u.enviarPendienteUseCase(n, d)).isNotNull();
        assertThat(u.reintentarFallidasUseCase(n, d, 3)).isNotNull();
    }

    @Test
    void rabbitCorreoYHilos() {
        InfraestructuraConfig c = new InfraestructuraConfig();

        assertThat(c.reloj("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(c.correoGateway(mock(JavaMailSender.class), "x@y.co", "smtp", "us-east-1"))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.correo.SmtpCorreoAdapter.class);
        assertThat(c.correoGateway(mock(JavaMailSender.class), "x@y.co", "SES", "us-east-1"))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.correo.SesCorreoAdapter.class);
        assertThat(c.sqsEventoConsumidor("https://sqs.us-east-1.amazonaws.com/123/cola", "us-east-1",
                mock(ProcesarEventoUseCase.class))).isNotNull();
        TopicExchange exchange = c.eventosExchange("mediagenda.eventos");
        Queue cola = c.colaNotificaciones("notificaciones.q", "notificaciones.dlq");
        assertThat(cola.getArguments()).containsEntry("x-dead-letter-routing-key", "notificaciones.dlq");
        assertThat(c.colaMuertos("notificaciones.dlq").getName()).isEqualTo("notificaciones.dlq");
        Binding enlace = c.enlaceNotificaciones(cola, exchange);
        assertThat(enlace.getRoutingKey()).isEqualTo("#");
        assertThat(c.eventoListener(mock(ProcesarEventoUseCase.class))).isNotNull();
        assertThat(c.correoExecutor().getMaxPoolSize()).isEqualTo(4);
        String api = "https://api.telegram.org";
        assertThat(c.smsGateway("sns", "us-east-1", api, "", ""))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.sms.SnsSmsAdapter.class);
        assertThat(c.smsGateway("simulado", "us-east-1", api, "", ""))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.sms.SmsSimuladoAdapter.class);
        assertThat(c.smsGateway("TELEGRAM", "us-east-1", api, " 123:abc ", " 999 "))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.sms.TelegramAdapter.class);
        // Telegram sin token o sin chat no se puede usar: se queda en simulado
        assertThat(c.smsGateway("telegram", "us-east-1", api, "", "999"))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.sms.SmsSimuladoAdapter.class);
        assertThat(c.smsGateway("telegram", "us-east-1", api, "123:abc", " "))
                .isInstanceOf(com.mediagenda.notificaciones.infrastructure.sms.SmsSimuladoAdapter.class);
        assertThat(c.solicitudEnvioGateway(mock(EnviarPendienteUseCase.class))).isNotNull();
    }
}
