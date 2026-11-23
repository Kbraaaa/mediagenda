package com.mediagenda.citas.infrastructure.ia;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.exception.TriageException;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API externa de IA: Google Gemini. Recibe SOLO el texto de los sintomas (nunca nombre ni documento)
 * y debe responder un JSON con especialidad, prioridad y motivo. Respeta un intervalo minimo entre
 * llamadas para no pasar el limite de la capa gratuita.
 */
public class GeminiTriageAdapter implements TriageGateway {

    private static final Logger log = LoggerFactory.getLogger(GeminiTriageAdapter.class);

    static final String INSTRUCCIONES = """
            Eres un asistente de triage para una IPS en Colombia. Lee los sintomas que escribio un paciente \
            y responde SOLO un JSON con esta forma: \
            {"especialidad": "<una de: %s>", "prioridad": "<BAJA|MEDIA|ALTA>", "motivo": "<maximo 200 caracteres>"}. \
            Usa ALTA solo si los sintomas sugieren una posible urgencia. No des diagnosticos. Sintomas: %s""";

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final long intervaloMinimoMs;
    private final JsonMapper json = JsonMapper.builder().build();
    private long ultimaLlamada;

    public GeminiTriageAdapter(RestClient.Builder builder, String urlBase, String apiKey, String modelo,
                               long intervaloMinimoMs) {
        this.restClient = builder.baseUrl(urlBase).build();
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.intervaloMinimoMs = intervaloMinimoMs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Candidato> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Contenido content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Contenido(List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Triage(String especialidad, String prioridad, String motivo) {
    }

    @Override
    public ResultadoTriage analizar(String sintomas) {
        esperarTurno();
        try {
            String especialidades = Arrays.stream(Especialidad.values()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            Map<String, Object> cuerpo = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text",
                            INSTRUCCIONES.formatted(especialidades, sintomas))))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0));
            Respuesta respuesta = restClient.post()
                    .uri("/v1beta/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Respuesta.class);
            String texto = respuesta.candidates().get(0).content().parts().get(0).text();
            Triage triage = json.readValue(texto, Triage.class);
            String motivo = triage.motivo() == null ? "" : triage.motivo();
            return new ResultadoTriage(Especialidad.desde(triage.especialidad()),
                    Prioridad.valueOf(triage.prioridad().trim().toUpperCase()),
                    motivo.length() > 500 ? motivo.substring(0, 500) : motivo);
        } catch (RuntimeException e) {
            // Se registra el motivo (modelo, error) para diagnosticar; nunca se escribe la clave
            log.warn("Gemini ({}) no entrego un triage valido: {}", modelo, e.getMessage());
            throw new TriageException("Gemini no entrego un triage valido: " + e.getMessage(), e);
        }
    }

    /** Si la ultima llamada fue hace menos del intervalo minimo, espera la diferencia. */
    private synchronized void esperarTurno() {
        long espera = ultimaLlamada + intervaloMinimoMs - System.currentTimeMillis();
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        ultimaLlamada = System.currentTimeMillis();
    }
}
