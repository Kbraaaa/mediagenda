package com.mediagenda.citas.infrastructure.tareas;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CalentamientoAlArrancarTest {

    @Test
    void abreConexionesYLlamaVariasVecesSinDetenerseAnteErrores() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        servidor.expect(ExpectedCount.times(3), requestTo("http://localhost:9/a")).andRespond(withSuccess());
        servidor.expect(ExpectedCount.times(3), requestTo("http://localhost:9/b")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        AtomicInteger conexiones = new AtomicInteger();
        Runnable falla = () -> {
            throw new IllegalStateException("sin red");
        };

        CalentamientoAlArrancar calentamiento = new CalentamientoAlArrancar(builder.build(), List.of("/a", "/b"), 3,
                List.of(falla, conexiones::incrementAndGet));

        assertThat(calentamiento.calentar()).isEqualTo(3);
        assertThat(conexiones.get()).isEqualTo(1);
        servidor.verify();
    }

    @Test
    void alArrancarCorreEnSegundoPlano() throws Exception {
        CalentamientoAlArrancar calentamiento = new CalentamientoAlArrancar(
                RestClient.builder().baseUrl("http://localhost:9").build(), List.of(), 1, List.of());

        assertThat(calentamiento.alArrancar().get()).isZero();
    }
}
