package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.CompatibilidadSanguinea;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.VerificacionAlergia;
import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.exception.ReglaNegocioException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Las 5 operaciones de negocio de ms-pacientes: camino feliz y cada regla que las rechaza. */
class OperacionesPacienteUseCaseTest {

    /** "Hoy" fijo para que la edad no dependa del dia en que se corre la prueba. */
    private static final Clock HOY = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneId.of("America/Bogota"));

    private PacienteGateway gateway;

    @BeforeEach
    void preparar() {
        gateway = mock(PacienteGateway.class);
        when(gateway.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void existe(Paciente p) {
        when(gateway.buscarPorCodigo(p.getCodigo())).thenReturn(Optional.of(p));
    }

    private static Paciente paciente(String codigo) {
        return PacientesDePrueba.guardado().toBuilder().codigo(codigo).build();
    }

    // ---------- Reactivacion ----------

    @Test
    void reactivaUnPacienteInactivo() {
        Paciente inactivo = paciente("PAC-000001").toBuilder().activo(false).build();
        existe(inactivo);

        Paciente r = new ReactivarPacienteUseCase(gateway).ejecutar("PAC-000001");

        assertThat(r.isActivo()).isTrue();
        verify(gateway).guardar(inactivo);
    }

    @Test
    void noReactivaUnPacienteQueYaEstaActivo() {
        existe(paciente("PAC-000001"));

        assertThatThrownBy(() -> new ReactivarPacienteUseCase(gateway).ejecutar("PAC-000001"))
                .isInstanceOf(ConflictoPacienteException.class)
                .extracting("codigoError").isEqualTo("PACIENTE_YA_ACTIVO");
        verify(gateway, never()).guardar(any());
    }

    @Test
    void reactivarPacienteInexistenteEs404() {
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ReactivarPacienteUseCase(gateway).ejecutar("PAC-999999"))
                .isInstanceOf(PacienteNoEncontradoException.class);
    }

    // ---------- Registrar alergia ----------

    @Test
    void agregaLaAlergiaALasExistentes() {
        existe(paciente("PAC-000001"));

        Paciente r = new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-000001", "  Sulfas ");

        assertThat(r.getAlergias()).isEqualTo("Penicilina, Sulfas");
    }

    @Test
    void registraLaPrimeraAlergia() {
        existe(paciente("PAC-000001").toBuilder().alergias(null).build());

        Paciente r = new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-000001", "Latex");

        assertThat(r.getAlergias()).isEqualTo("Latex");
    }

    @Test
    void noRepiteUnaAlergiaAunqueCambienMayusculasOTildes() {
        existe(paciente("PAC-000001").toBuilder().alergias("Penicilina, Acido acetilsalicilico").build());

        assertThatThrownBy(() -> new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-000001", "ÁCIDO ACETILSALICÍLICO"))
                .isInstanceOf(ConflictoPacienteException.class)
                .extracting("codigoError").isEqualTo("ALERGIA_YA_REGISTRADA");
    }

    @Test
    void noRegistraAlergiaAPacienteInactivo() {
        existe(paciente("PAC-000001").toBuilder().activo(false).build());

        assertThatThrownBy(() -> new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-000001", "Latex"))
                .isInstanceOf(PacienteInactivoException.class);
    }

    @Test
    void rechazaAlergiaVaciaCortaOLarga() {
        RegistrarAlergiaUseCase uc = new RegistrarAlergiaUseCase(gateway);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "ab")).hasMessageContaining("entre 3 y 60");
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "x".repeat(61))).hasMessageContaining("entre 3 y 60");
    }

    @Test
    void noSuperaElLimiteTotalDeAlergias() {
        existe(paciente("PAC-000001").toBuilder().alergias("a".repeat(495)).build());

        assertThatThrownBy(() -> new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-000001", "Latex"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void registrarAlergiaAPacienteInexistenteEs404() {
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistrarAlergiaUseCase(gateway).ejecutar("PAC-999999", "Latex"))
                .isInstanceOf(PacienteNoEncontradoException.class);
    }

    // ---------- Verificar alergia ----------

    @Test
    void alertaPorMedicamentoDeLaMismaFamilia() {
        existe(paciente("PAC-000001"));

        VerificacionAlergia v = new VerificarAlergiaUseCase(gateway).ejecutar("PAC-000001", " Amoxicilina ");

        assertThat(v.alerta()).isTrue();
        assertThat(v.sustancia()).isEqualTo("Amoxicilina");
        assertThat(v.coincidencias()).containsExactly("Penicilina (misma familia: penicilinas)");
    }

    @Test
    void sinAlertaCuandoNoHayCoincidencias() {
        existe(paciente("PAC-000001").toBuilder().alergias(null).build());

        VerificacionAlergia v = new VerificarAlergiaUseCase(gateway).ejecutar("PAC-000001", "Loratadina");

        assertThat(v.alerta()).isFalse();
        assertThat(v.coincidencias()).isEmpty();
    }

    @Test
    void verificarExigeSustanciaYPacienteExistente() {
        VerificarAlergiaUseCase uc = new VerificarAlergiaUseCase(gateway);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", null)).isInstanceOf(IllegalArgumentException.class);
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> uc.ejecutar("PAC-999999", "Ibuprofeno")).isInstanceOf(PacienteNoEncontradoException.class);
    }

    // ---------- Cambio de documento ----------

    private Paciente conTi(LocalDate nacimiento) {
        return paciente("PAC-000002").toBuilder().tipoDocumento(TipoDocumento.TI).numeroDocumento("1012345678")
                .fechaNacimiento(nacimiento).build();
    }

    @Test
    void cambiaTarjetaDeIdentidadPorCedulaAlCumplir18() {
        existe(conTi(LocalDate.of(2008, 11, 25)));   // cumple 18 justo "hoy"
        when(gateway.existeDocumento(TipoDocumento.CC, "1012345678")).thenReturn(false);

        Paciente r = new CambiarDocumentoUseCase(gateway, HOY).ejecutar("PAC-000002", " 1012345678 ");

        assertThat(r.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
        assertThat(r.getNumeroDocumento()).isEqualTo("1012345678");
    }

    @Test
    void noCambiaDocumentoDeUnMenorDeEdad() {
        existe(conTi(LocalDate.of(2008, 11, 26)));   // cumple 18 manana

        assertThatThrownBy(() -> new CambiarDocumentoUseCase(gateway, HOY).ejecutar("PAC-000002", "1012345678"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("17")
                .extracting("codigoError").isEqualTo("MENOR_DE_EDAD");
    }

    @Test
    void soloSeCambiaLaTarjetaDeIdentidad() {
        existe(paciente("PAC-000001"));   // tiene CC

        assertThatThrownBy(() -> new CambiarDocumentoUseCase(gateway, HOY).ejecutar("PAC-000001", "1012345678"))
                .isInstanceOf(ConflictoPacienteException.class)
                .extracting("codigoError").isEqualTo("DOCUMENTO_NO_ES_TI");
    }

    @Test
    void laCedulaNuevaNoPuedeExistir() {
        existe(conTi(LocalDate.of(2000, 1, 1)));
        when(gateway.existeDocumento(TipoDocumento.CC, "1069123456")).thenReturn(true);

        assertThatThrownBy(() -> new CambiarDocumentoUseCase(gateway, HOY).ejecutar("PAC-000002", "1069123456"))
                .isInstanceOf(PacienteDuplicadoException.class);
    }

    @Test
    void cambioDeDocumentoValidaNumeroEstadoYExistencia() {
        CambiarDocumentoUseCase uc = new CambiarDocumentoUseCase(gateway, HOY);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000002", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000002", "12AB45")).isInstanceOf(IllegalArgumentException.class);
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> uc.ejecutar("PAC-999999", "1012345678")).isInstanceOf(PacienteNoEncontradoException.class);
        existe(conTi(LocalDate.of(2000, 1, 1)).toBuilder().activo(false).build());
        assertThatThrownBy(() -> uc.ejecutar("PAC-000002", "1012345678")).isInstanceOf(PacienteInactivoException.class);
    }

    // ---------- Compatibilidad sanguinea ----------

    @Test
    void evaluaCompatibilidadEntreDosPacientes() {
        existe(paciente("PAC-000001").toBuilder().tipoSangre("AB+").build());
        existe(paciente("PAC-000002").toBuilder().tipoSangre("O-").build());

        CompatibilidadSanguinea c = new CompatibilidadSanguineaUseCase(gateway).ejecutar("PAC-000001", " PAC-000002 ");

        assertThat(c.compatible()).isTrue();
        assertThat(c.tipoReceptor()).isEqualTo("AB+");
        assertThat(c.tipoDonante()).isEqualTo("O-");
    }

    @Test
    void exigeTipoDeSangreRegistradoEnAmbos() {
        existe(paciente("PAC-000001"));
        existe(paciente("PAC-000002").toBuilder().tipoSangre(null).build());
        existe(paciente("PAC-000003").toBuilder().tipoSangre(" ").build());
        CompatibilidadSanguineaUseCase uc = new CompatibilidadSanguineaUseCase(gateway);

        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "PAC-000002"))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigoError").isEqualTo("TIPO_SANGRE_NO_REGISTRADO");
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "PAC-000003")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void compatibilidadValidaDonanteYExistencia() {
        CompatibilidadSanguineaUseCase uc = new CompatibilidadSanguineaUseCase(gateway);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uc.ejecutar("PAC-000001", "pac-000001")).hasMessageContaining("distintos");
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> uc.ejecutar("PAC-999999", "PAC-000001")).isInstanceOf(PacienteNoEncontradoException.class);
    }
}
