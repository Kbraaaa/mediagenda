package com.mediagenda.citas.infrastructure.rest_client;

import com.mediagenda.citas.domain.model.DatosPaciente;
import com.mediagenda.citas.domain.model.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** ms-pacientes y Nager.Date simulados con MockRestServiceServer (sin red). */
class ClientesRestTest {

    // ---------- ms-pacientes ----------

    @Test
    void pacienteExistente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://pacientes/api/v1/pacientes/PAC-000001"))
                .andRespond(withSuccess("""
                        {"codigo":"PAC-000001","nombres":"Laura","apellidos":"Gomez",
                         "correo":"laura@correo.com","telefono":"3101234567","activo":true,"tipoSangre":"O+"}""", MediaType.APPLICATION_JSON));

        Optional<DatosPaciente> paciente = new PacienteRestAdapter(builder, "http://pacientes").buscar("PAC-000001");

        assertThat(paciente).contains(new DatosPaciente("PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567", true));
        servidor.verify();
    }

    @Test
    void pacienteInexistenteEsVacio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://pacientes/api/v1/pacientes/PAC-000404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(new PacienteRestAdapter(builder, "http://pacientes").buscar("PAC-000404")).isEmpty();
    }

    @Test
    void errorDelServidorEs503() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://pacientes/api/v1/pacientes/PAC-000001")).andRespond(withServerError());

        assertThatThrownBy(() -> new PacienteRestAdapter(builder, "http://pacientes").buscar("PAC-000001"))
                .isInstanceOf(ServicioNoDisponibleException.class).hasMessageContaining("500");
    }

    @Test
    void tiempoAgotadoEs503() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://pacientes/api/v1/pacientes/PAC-000001"))
                .andRespond(withException(new SocketTimeoutException("lento")));

        assertThatThrownBy(() -> new PacienteRestAdapter(builder, "http://pacientes").buscar("PAC-000001"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ---------- Calculo local de festivos (Ley 51 de 1983) ----------

    @Test
    void festivosDeColombia2026() {
        assertThat(FestivosColombia.calcular(2026)).containsExactly(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25));
    }

    @Test
    void pascuaYLunesSiguiente() {
        assertThat(FestivosColombia.domingoDePascua(2025)).isEqualTo(LocalDate.of(2025, 4, 20));
        assertThat(FestivosColombia.domingoDePascua(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(FestivosColombia.domingoDePascua(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(FestivosColombia.lunesSiguiente(LocalDate.of(2026, 10, 12))).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(FestivosColombia.lunesSiguiente(LocalDate.of(2026, 11, 11))).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    // ---------- Nager.Date ----------

    @Test
    void festivosSeConsultanUnaVezPorAnio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(once(), requestTo("https://nager/api/v3/PublicHolidays/2026/CO"))
                .andRespond(withSuccess("""
                        [{"date":"2026-12-08","localName":"Inmaculada Concepcion","name":"Immaculate Conception"},
                         {"date":"2026-12-25","localName":"Navidad","name":"Christmas Day"}]""", MediaType.APPLICATION_JSON));
        FestivosNagerAdapter festivos = new FestivosNagerAdapter(builder, "https://nager", "CO");

        assertThat(festivos.esFestivo(LocalDate.of(2026, 12, 8))).isTrue();
        assertThat(festivos.esFestivo(LocalDate.of(2026, 12, 9))).isFalse();
        assertThat(festivos.esFestivo(LocalDate.of(2026, 12, 25))).isTrue();
        servidor.verify();
    }

    @Test
    void siNagerFallaSeUsaElCalculoLocal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(once(), requestTo("https://nager/api/v3/PublicHolidays/2027/CO")).andRespond(withServerError());
        FestivosNagerAdapter festivos = new FestivosNagerAdapter(builder, "https://nager", "CO");

        assertThat(festivos.esFestivo(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(festivos.esFestivo(LocalDate.of(2027, 1, 5))).isFalse();
        servidor.verify();
    }

    @Test
    void precargaLosFestivosAlArrancar() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(once(), requestTo("https://nager/api/v3/PublicHolidays/2026/CO"))
                .andRespond(withSuccess("[{\"date\":\"2026-12-25\",\"localName\":\"Navidad\"}]", MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo("https://nager/api/v3/PublicHolidays/2027/CO")).andRespond(withServerError());
        FestivosNagerAdapter festivos = new FestivosNagerAdapter(builder, "https://nager", "CO");

        festivos.precargar(2026, 2027);

        assertThat(festivos.esFestivo(LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(festivos.esFestivo(LocalDate.of(2027, 12, 8))).isTrue();
        servidor.verify();
    }

    @Test
    void respuestaVaciaDeNager() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://nager/api/v3/PublicHolidays/2028/CO"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        servidor.expect(requestTo("https://nager/api/v3/PublicHolidays/2029/CO"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        FestivosNagerAdapter festivos = new FestivosNagerAdapter(builder, "https://nager", "CO");

        assertThat(festivos.esFestivo(LocalDate.of(2028, 1, 1))).isTrue();
        assertThat(festivos.esFestivo(LocalDate.of(2029, 1, 1))).isTrue();
    }
}
