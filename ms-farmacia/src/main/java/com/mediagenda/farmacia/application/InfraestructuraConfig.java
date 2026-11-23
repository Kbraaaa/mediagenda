package com.mediagenda.farmacia.application;

import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import com.mediagenda.farmacia.infrastructure.eventos.RabbitEventoAdapter;
import com.mediagenda.farmacia.infrastructure.eventos.SqsEventoAdapter;
import com.mediagenda.farmacia.infrastructure.tareas.CalentamientoAlArrancar;
import com.mediagenda.farmacia.infrastructure.jpa_driver_adapter.TransaccionAdapter;
import com.mediagenda.farmacia.infrastructure.rest_client.CitaRestAdapter;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class InfraestructuraConfig {

    @Bean
    public Clock reloj(@Value("${farmacia.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    public CitaGateway citaGateway(@Value("${citas.service.url}") String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(1));
        fabrica.setReadTimeout(Duration.ofSeconds(2));
        return new CitaRestAdapter(RestClient.builder().requestFactory(fabrica), url);
    }

    @Bean
    public TransaccionGateway transaccionGateway(PlatformTransactionManager transactionManager) {
        return new TransaccionAdapter(new TransactionTemplate(transactionManager));
    }

    @Bean
    public TopicExchange eventosExchange(@Value("${mediagenda.eventos.exchange}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    /**
     * Local: RabbitMQ. AWS: Amazon SQS (mediagenda.mensajeria=sqs). Las credenciales de AWS no van
     * en el codigo: el SDK las toma del rol de IAM de la instancia EC2.
     */
    @Bean
    public EventoGateway eventoGateway(RabbitTemplate rabbitTemplate,
                                       @Value("${mediagenda.eventos.exchange}") String exchange,
                                       @Value("${mediagenda.mensajeria:rabbit}") String mensajeria,
                                       @Value("${aws.sqs.cola-url:}") String colaUrl,
                                       @Value("${aws.region:us-east-2}") String region) {
        if ("sqs".equalsIgnoreCase(mensajeria)) {
            return new SqsEventoAdapter(SqsClient.builder().region(Region.of(region)).build(), colaUrl);
        }
        return new RabbitEventoAdapter(rabbitTemplate, exchange);
    }

    /** Calentamiento al arrancar: conexiones externas + consultas propias (solo lectura). */
    @Bean
    public CalentamientoAlArrancar calentamiento(EventoGateway eventos, CitaGateway citaGateway,
                                                 @Value("${server.port:8083}") int puerto,
                                                 @Value("${calentamiento.repeticiones:30}") int repeticiones) {
        List<Runnable> conexiones = new ArrayList<>();
        conexiones.add(() -> citaGateway.buscar("CIT-2000-000000"));
        if (eventos instanceof SqsEventoAdapter sqs) {
            conexiones.add(sqs::precalentar);
        }
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(1));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        RestClient propio = RestClient.builder().requestFactory(fabrica).baseUrl("http://localhost:" + puerto).build();
        return new CalentamientoAlArrancar(propio, List.of("/api/v1/medicamentos?size=5", "/api/v1/formulas?size=5", "/api/v1/medicamentos/MDT-0001", "/api/v1/formulas/FOR-2000-000000"), repeticiones, conexiones);
    }
}
