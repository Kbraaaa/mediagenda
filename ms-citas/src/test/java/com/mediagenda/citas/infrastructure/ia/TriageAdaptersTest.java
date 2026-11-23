package com.mediagenda.citas.infrastructure.ia;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.exception.TriageException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TriageAdaptersTest {

    private static final String URL = "https://gemini/v1beta/models/gemini-3.5-flash-lite:generateContent";

    private static String respuestaGemini(String jsonInterno) {
        String escapado = jsonInterno.replace("\"", "\\\"");
        return """
                {"candidates":[{"content":{"parts":[{"text":"%s"}],"role":"model"},"finishReason":"STOP"}],
                 "usageMetadata":{"totalTokenCount":120}}""".formatted(escapado);
    }

    @Test
    void geminiDevuelveElTriage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-prueba"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("me duele el pecho")))
                .andRespond(withSuccess(respuestaGemini(
                        "{\"especialidad\":\"cardiologia\",\"prioridad\":\"alta\",\"motivo\":\"dolor toracico\"}"),
                        MediaType.APPLICATION_JSON));

        ResultadoTriage resultado = new GeminiTriageAdapter(builder, "https://gemini", "clave-prueba",
                "gemini-3.5-flash-lite", 0).analizar("me duele el pecho y me falta el aire");

        assertThat(resultado).isEqualTo(new ResultadoTriage(Especialidad.CARDIOLOGIA, Prioridad.ALTA, "dolor toracico"));
        servidor.verify();
    }

    @Test
    void geminiRecortaMotivosLargosYAceptaMotivoVacio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withSuccess(respuestaGemini(
                "{\"especialidad\":\"NEUROLOGIA\",\"prioridad\":\"MEDIA\",\"motivo\":\"" + "x".repeat(600) + "\"}"),
                MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL)).andRespond(withSuccess(respuestaGemini(
                "{\"especialidad\":\"NEUROLOGIA\",\"prioridad\":\"BAJA\"}"), MediaType.APPLICATION_JSON));
        GeminiTriageAdapter adapter = new GeminiTriageAdapter(builder, "https://gemini", "k", "gemini-3.5-flash-lite", 20);

        assertThat(adapter.analizar("dolor de cabeza").motivo()).hasSize(500);
        assertThat(adapter.analizar("dolor de cabeza").motivo()).isEmpty();
    }

    @Test
    void geminiConErrorORespuestaInvalidaLanzaTriageException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withSuccess(respuestaGemini(
                "{\"especialidad\":\"ASTROLOGIA\",\"prioridad\":\"ALTA\",\"motivo\":\"x\"}"), MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));
        GeminiTriageAdapter adapter = new GeminiTriageAdapter(builder, "https://gemini", "k", "gemini-3.5-flash-lite", 0);

        assertThatThrownBy(() -> adapter.analizar("sintomas")).isInstanceOf(TriageException.class);
        assertThatThrownBy(() -> adapter.analizar("sintomas")).isInstanceOf(TriageException.class);
        assertThatThrownBy(() -> adapter.analizar("sintomas")).isInstanceOf(TriageException.class);
    }

    @Test
    void esperaInterrumpidaNoRompeElTriage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        GeminiTriageAdapter adapter = new GeminiTriageAdapter(builder, "https://gemini", "k", "gemini-3.5-flash-lite", 5000);

        assertThatThrownBy(() -> adapter.analizar("uno")).isInstanceOf(TriageException.class);
        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> adapter.analizar("dos")).isInstanceOf(TriageException.class);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void triagePorReglas() {
        TriagePorReglasAdapter reglas = new TriagePorReglasAdapter();

        assertThat(reglas.analizar("Tengo DOLOR EN EL PECHO y me desmayé"))
                .isEqualTo(new ResultadoTriage(Especialidad.CARDIOLOGIA, Prioridad.ALTA,
                        "Clasificacion por palabras clave (sin IA)"));
        assertThat(reglas.analizar("Me salió un brote en la piel").especialidad()).isEqualTo(Especialidad.DERMATOLOGIA);
        assertThat(reglas.analizar("Me salió un brote en la piel").prioridad()).isEqualTo(Prioridad.MEDIA);
        assertThat(reglas.analizar("Me siento cansado").especialidad()).isEqualTo(Especialidad.MEDICINA_GENERAL);
        assertThat(reglas.analizar("Me siento cansado").prioridad()).isEqualTo(Prioridad.BAJA);
        assertThat(TriagePorReglasAdapter.normalizar("Corazón")).isEqualTo("corazon");
    }
}
