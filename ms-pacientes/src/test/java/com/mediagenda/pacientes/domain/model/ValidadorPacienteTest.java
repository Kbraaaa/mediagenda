package com.mediagenda.pacientes.domain.model;

import com.mediagenda.pacientes.PacientesDePrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorPacienteTest {

    private static void debeFallar(Paciente paciente, String mensaje) {
        assertThatThrownBy(() -> ValidadorPaciente.validar(paciente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }

    @Test
    void aceptaDatosCompletos() {
        assertThatCode(() -> ValidadorPaciente.validar(PacientesDePrueba.datosValidos()))
                .doesNotThrowAnyException();
    }

    @Test
    void aceptaCamposOpcionalesVacios() {
        Paciente sinOpcionales = PacientesDePrueba.datosValidos().toBuilder()
                .telefono(null).tipoSangre(null).alergias(null).build();
        assertThatCode(() -> ValidadorPaciente.validar(sinOpcionales)).doesNotThrowAnyException();
    }

    @Test
    void rechazaPacienteNulo() {
        debeFallar(null, "obligatorios");
    }

    @Test
    void rechazaSinTipoDocumento() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().tipoDocumento(null).build(), "tipo de documento");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rechazaDocumentoVacio(String documento) {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().numeroDocumento(documento).build(), "obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12.345.678", "123456789012345678901"})
    void rechazaDocumentoMalFormado(String documento) {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().numeroDocumento(documento).build(), "entre 5 y 20");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaNombresVacios(String nombres) {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().nombres(nombres).build(), "nombres es obligatorio");
    }

    @Test
    void rechazaNombresLargos() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().nombres("a".repeat(81)).build(), "no puede superar 80");
    }

    @Test
    void rechazaApellidosVacios() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().apellidos(" ").build(), "apellidos es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba.com", "dos@@correo.com", "con espacio@correo.com", "falta@dominio"})
    void rechazaCorreoInvalido(String correo) {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().correo(correo).build(), "formato valido");
    }

    @Test
    void rechazaCorreoLargo() {
        String largo = "a".repeat(115) + "@x.com";
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().correo(largo).build(), "120");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "telefono", "310-123-4567"})
    void rechazaTelefonoInvalido(String telefono) {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().telefono(telefono).build(), "telefono");
    }

    @Test
    void rechazaSinFechaNacimiento() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().fechaNacimiento(null).build(), "obligatoria");
    }

    @Test
    void rechazaFechaFutura() {
        Paciente futuro = PacientesDePrueba.datosValidos().toBuilder()
                .fechaNacimiento(LocalDate.now().plusDays(1)).build();
        debeFallar(futuro, "futura");
    }

    @Test
    void rechazaFechaDemasiadoAntigua() {
        Paciente antiguo = PacientesDePrueba.datosValidos().toBuilder()
                .fechaNacimiento(LocalDate.now().minusYears(121)).build();
        debeFallar(antiguo, "no es valida");
    }

    @Test
    void rechazaTipoSangreInvalido() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().tipoSangre("C+").build(), "Tipo de sangre");
    }

    @Test
    void rechazaAlergiasLargas() {
        debeFallar(PacientesDePrueba.datosValidos().toBuilder().alergias("x".repeat(501)).build(), "500");
    }
}
