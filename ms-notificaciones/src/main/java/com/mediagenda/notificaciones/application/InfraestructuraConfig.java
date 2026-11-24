package com.mediagenda.notificaciones.application;

import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import com.mediagenda.notificaciones.domain.usecase.EnviarPendienteUseCase;
import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import com.mediagenda.notificaciones.infrastructure.correo.SesCorreoAdapter;
import com.mediagenda.notificaciones.infrastructure.correo.SmtpCorreoAdapter;
import com.mediagenda.notificaciones.infrastructure.eventos.EventoListener;
import com.mediagenda.notificaciones.infrastructure.eventos.SqsEventoConsumidor;
import com.mediagenda.notificaciones.infrastructure.sms.SmsSimuladoAdapter;
import com.mediagenda.notificaciones.infrastructure.sms.SnsSmsAdapter;
import com.mediagenda.notificaciones.infrastructure.sms.TelegramAdapter;
import com.mediagenda.notificaciones.infrastructure.tareas.SolicitudEnvioAsincrona;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Local: RabbitMQ y correo SMTP (Mailpit). AWS (perfil "aws"): Amazon SQS y Amazon SES.
 * El dominio es el mismo en los dos casos; solo cambia el adaptador que se conecta al puerto.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class InfraestructuraConfig {

    @Bean
    public Clock reloj(@Value("${notificaciones.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    public CorreoGateway correoGateway(JavaMailSender mailSender,
                                       @Value("${notificaciones.remitente}") String remitente,
                                       @Value("${notificaciones.correo:smtp}") String proveedor,
                                       @Value("${aws.region:us-east-2}") String region) {
        if ("ses".equalsIgnoreCase(proveedor)) {
            return new SesCorreoAdapter(SesV2Client.builder().region(Region.of(region)).build(), remitente);
        }
        return new SmtpCorreoAdapter(mailSender, remitente);
    }

    @Bean
    public TopicExchange eventosExchange(@Value("${mediagenda.eventos.exchange}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    /** Si un mensaje se rechaza, RabbitMQ lo pasa a la cola de mensajes muertos (DLQ) para revisarlo. */
    @Bean
    public Queue colaNotificaciones(@Value("${notificaciones.cola}") String cola,
                                    @Value("${notificaciones.cola-muertos}") String muertos) {
        return QueueBuilder.durable(cola).deadLetterExchange("").deadLetterRoutingKey(muertos).build();
    }

    @Bean
    public Queue colaMuertos(@Value("${notificaciones.cola-muertos}") String muertos) {
        return QueueBuilder.durable(muertos).build();
    }

    /** "#" = todos los eventos del exchange (cita.* y formula.*). */
    @Bean
    public Binding enlaceNotificaciones(Queue colaNotificaciones, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaNotificaciones).to(eventosExchange).with("#");
    }

    @Bean
    public EventoListener eventoListener(ProcesarEventoUseCase procesarEvento) {
        return new EventoListener(procesarEvento);
    }

    /** sns: Amazon SNS. telegram: bot de Telegram (canal movil real). simulado o ninguno: en el log. */
    @Bean
    public SmsGateway smsGateway(@Value("${notificaciones.sms:simulado}") String proveedor,
                                 @Value("${aws.region:us-east-2}") String region,
                                 @Value("${telegram.url:https://api.telegram.org}") String telegramUrl,
                                 @Value("${telegram.token:}") String telegramToken,
                                 @Value("${telegram.chat-id:}") String telegramChat) {
        if ("sns".equalsIgnoreCase(proveedor)) {
            return new SnsSmsAdapter(SnsClient.builder().region(Region.of(region)).build());
        }
        if ("telegram".equalsIgnoreCase(proveedor) && !telegramToken.isBlank() && !telegramChat.isBlank()) {
            SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
            fabrica.setConnectTimeout(Duration.ofSeconds(3));
            fabrica.setReadTimeout(Duration.ofSeconds(10));
            return new TelegramAdapter(RestClient.builder().requestFactory(fabrica), telegramUrl,
                    telegramToken.trim(), telegramChat.trim());
        }
        return new SmsSimuladoAdapter();
    }

    /** Solo en AWS: lee los eventos desde Amazon SQS en lugar de RabbitMQ. */
    @Bean
    @ConditionalOnProperty(name = "mediagenda.mensajeria", havingValue = "sqs")
    public SqsEventoConsumidor sqsEventoConsumidor(@Value("${aws.sqs.cola-url}") String colaUrl,
                                                   @Value("${aws.region:us-east-2}") String region,
                                                   ProcesarEventoUseCase procesarEvento) {
        return new SqsEventoConsumidor(SqsClient.builder().region(Region.of(region)).build(), colaUrl, procesarEvento);
    }

    @Bean(name = "correoExecutor")
    public ThreadPoolTaskExecutor correoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("correo-");
        return executor;
    }

    @Bean
    public SolicitudEnvioGateway solicitudEnvioGateway(EnviarPendienteUseCase enviarPendiente) {
        return new SolicitudEnvioAsincrona(enviarPendiente);
    }
}
