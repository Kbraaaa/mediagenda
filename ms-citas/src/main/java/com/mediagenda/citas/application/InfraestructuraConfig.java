package com.mediagenda.citas.application;

import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import com.mediagenda.citas.domain.usecase.ProcesarTriageUseCase;
import com.mediagenda.citas.infrastructure.eventos.RabbitEventoAdapter;
import com.mediagenda.citas.infrastructure.eventos.SqsEventoAdapter;
import com.mediagenda.citas.infrastructure.tareas.CalentamientoAlArrancar;
import com.mediagenda.citas.infrastructure.ia.GeminiTriageAdapter;
import com.mediagenda.citas.infrastructure.ia.TriagePorReglasAdapter;
import com.mediagenda.citas.infrastructure.rest_client.FestivosNagerAdapter;
import com.mediagenda.citas.infrastructure.rest_client.PacienteRestAdapter;
import com.mediagenda.citas.infrastructure.tareas.SolicitudTriageAsincrona;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.time.ZoneId;

/** Adaptadores de salida: clientes REST, IA, RabbitMQ, reloj y el hilo del triage. */
@Configuration
@EnableAsync
@EnableScheduling
public class InfraestructuraConfig {

    @Bean
    public Clock reloj(@Value("${citas.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    public PacienteGateway pacienteGateway(@Value("${pacientes.service.url}") String url) {
        return new PacienteRestAdapter(clienteHttp(Duration.ofSeconds(1), Duration.ofSeconds(2)), url);
    }

    /**
     * Al arrancar se precargan los festivos del anio actual y el siguiente en un hilo aparte:
     * asi ninguna peticion de un usuario espera a la API externa.
     */
    @Bean
    public FestivoGateway festivoGateway(@Value("${festivos.url}") String url,
                                         @Value("${festivos.pais:CO}") String pais, Clock reloj) {
        FestivosNagerAdapter festivos =
                new FestivosNagerAdapter(clienteHttp(Duration.ofSeconds(2), Duration.ofSeconds(5)), url, pais);
        int anio = LocalDate.now(reloj).getYear();
        CompletableFuture.runAsync(() -> festivos.precargar(anio, anio + 1));
        return festivos;
    }

    /** Con clave de Gemini se usa la IA; sin clave (o desactivada) se usa el triage por reglas. */
    @Bean
    public TriageGateway triageGateway(@Value("${gemini.habilitado:true}") boolean habilitado,
                                       @Value("${gemini.api-key:}") String apiKey,
                                       @Value("${gemini.modelo}") String modelo,
                                       @Value("${gemini.url}") String url,
                                       @Value("${gemini.intervalo-minimo-ms:4000}") long intervalo) {
        if (habilitado && apiKey != null && !apiKey.isBlank()) {
            return new GeminiTriageAdapter(clienteHttp(Duration.ofSeconds(3), Duration.ofSeconds(15)),
                    url, apiKey.trim(), modelo, intervalo);
        }
        return new TriagePorReglasAdapter();
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

    /** Un solo hilo para el triage: las llamadas a la IA quedan en fila y respetan el limite gratuito. */
    @Bean(name = "triageExecutor")
    public ThreadPoolTaskExecutor triageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("triage-");
        return executor;
    }

    @Bean
    public SolicitudTriageGateway solicitudTriageGateway(ProcesarTriageUseCase procesarTriage) {
        return new SolicitudTriageAsincrona(procesarTriage);
    }

    static RestClient.Builder clienteHttp(Duration conectar, Duration leer) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(conectar);
        fabrica.setReadTimeout(leer);
        return RestClient.builder().requestFactory(fabrica);
    }

    /** Calentamiento al arrancar: conexiones externas + consultas propias (solo lectura). */
    @Bean
    public CalentamientoAlArrancar calentamiento(EventoGateway eventos, PacienteGateway pacienteGateway,
                                                 @Value("${server.port:8082}") int puerto,
                                                 @Value("${calentamiento.repeticiones:30}") int repeticiones) {
        List<Runnable> conexiones = new ArrayList<>();
        conexiones.add(() -> pacienteGateway.buscar("PAC-000000"));
        if (eventos instanceof SqsEventoAdapter sqs) {
            conexiones.add(sqs::precalentar);
        }
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(1));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        RestClient propio = RestClient.builder().requestFactory(fabrica).baseUrl("http://localhost:" + puerto).build();
        return new CalentamientoAlArrancar(propio, List.of("/api/v1/medicos?size=5", "/api/v1/franjas?size=5", "/api/v1/citas?size=5", "/api/v1/medicos/MED-0000"), repeticiones, conexiones);
    }
}
