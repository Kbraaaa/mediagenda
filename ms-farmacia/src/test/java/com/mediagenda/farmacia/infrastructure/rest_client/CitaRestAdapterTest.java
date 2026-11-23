package com.mediagenda.farmacia.infrastructure.rest_client;

import com.mediagenda.farmacia.domain.model.DatosCita;
import com.mediagenda.farmacia.domain.model.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CitaRestAdapterTest {

    private static final String URL = "http://citas/api/v1/citas/CIT-2026-000001";

    @Test
    void citaExistente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"codigo":"CIT-2026-000001","codigoPaciente":"PAC-000001","nombrePaciente":"Laura Gomez",
                 "correoPaciente":"laura@correo.com","telefonoPaciente":"3101234567","estado":"ATENDIDA","franja":{"idFranja":10}}""",
                MediaType.APPLICATION_JSON));

        assertThat(new CitaRestAdapter(builder, "http://citas").buscar("CIT-2026-000001")).contains(
                new DatosCita("CIT-2026-000001", "PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567", "ATENDIDA"));
    }

    @Test
    void citaInexistente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(new CitaRestAdapter(builder, "http://citas").buscar("CIT-2026-000001")).isEmpty();
    }

    @Test
    void errorOTiempoAgotadoEs503() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("lento")));
        CitaRestAdapter adapter = new CitaRestAdapter(builder, "http://citas");

        assertThatThrownBy(() -> adapter.buscar("CIT-2026-000001")).isInstanceOf(ServicioNoDisponibleException.class);
        assertThatThrownBy(() -> adapter.buscar("CIT-2026-000001")).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
