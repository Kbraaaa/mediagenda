package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.Datos;
import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.exception.NoEncontradoException;
import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reenvio y correccion de destinatario de avisos. */
class OperacionesNotificacionUseCaseTest {

    private NotificacionGateway notificaciones;
    private SolicitudEnvioGateway solicitud;
    private ReenviarNotificacionUseCase reenviar;
    private CorregirDestinatarioUseCase corregir;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionGateway.class);
        solicitud = mock(SolicitudEnvioGateway.class);
        Despachador despachador = new Despachador(mock(CorreoGateway.class), mock(SmsGateway.class), notificaciones, Datos.RELOJ);
        reenviar = new ReenviarNotificacionUseCase(notificaciones, solicitud, despachador);
        corregir = new CorregirDestinatarioUseCase(notificaciones, solicitud);
        when(notificaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(notificaciones.siguienteConsecutivo()).thenReturn(7L);
    }

    private Notificacion existe(EstadoNotificacion estado) {
        Notificacion n = Datos.notificacion(estado);
        when(notificaciones.buscarPorCodigo("NOT-2026-000001")).thenReturn(Optional.of(n));
        return n;
    }

    // ---------- Reenvio ----------

    @ParameterizedTest
    @EnumSource(value = EstadoNotificacion.class, names = {"ENVIADA", "LEIDA"})
    void reenviaUnaCopiaNuevaYDejaLaOriginalIgual(EstadoNotificacion estado) {
        Notificacion original = existe(estado);

        Notificacion copia = reenviar.ejecutar("NOT-2026-000001");

        assertThat(copia.getCodigo()).isEqualTo("NOT-2026-000007");
        assertThat(copia.getIdNotificacion()).isNotEqualTo(original.getIdNotificacion());
        assertThat(copia.getEventoId()).isNull();
        assertThat(copia.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(copia.getIntentos()).isZero();
        assertThat(copia.getDestinatario()).isEqualTo(original.getDestinatario());
        assertThat(copia.getCuerpo()).isEqualTo(original.getCuerpo());
        assertThat(copia.getTipo()).isEqualTo("cita.agendada");
        assertThat(copia.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 11, 20, 10, 0));
        assertThat(original.getEstado()).isEqualTo(estado);
        verify(solicitud).solicitar("NOT-2026-000007");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoNotificacion.class, names = {"PENDIENTE", "FALLIDA"})
    void noReenviaLoQueNoHaSalido(EstadoNotificacion estado) {
        existe(estado);

        assertThatThrownBy(() -> reenviar.ejecutar("NOT-2026-000001"))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("NOTIFICACION_NO_ENTREGADA");
        verify(solicitud, never()).solicitar(anyString());
    }

    @Test
    void reenviarNotificacionInexistenteEs404() {
        when(notificaciones.buscarPorCodigo("NOT-2026-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reenviar.ejecutar("NOT-2026-999999")).isInstanceOf(NoEncontradoException.class);
    }

    // ---------- Correccion de destinatario ----------

    @Test
    void corrigeElCorreoYVuelveAEnviarDesdeCero() {
        Notificacion n = existe(EstadoNotificacion.FALLIDA);
        n.setIntentos(3);
        n.setUltimoError("Buzon inexistente");

        Notificacion r = corregir.ejecutar("NOT-2026-000001", " laura.gomez@correo.com ");

        assertThat(r.getDestinatario()).isEqualTo("laura.gomez@correo.com");
        assertThat(r.getIntentos()).isZero();
        assertThat(r.getUltimoError()).isNull();
        assertThat(r.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        verify(solicitud).solicitar("NOT-2026-000001");
    }

    @Test
    void enAvisosAlCelularGuardaElNumeroEnFormatoInternacional() {
        existe(EstadoNotificacion.FALLIDA).setCanal(Canal.SMS);

        assertThat(corregir.ejecutar("NOT-2026-000001", "310 123 4567").getDestinatario()).isEqualTo("+573101234567");
    }

    @Test
    void validaElDestinatarioSegunElCanal() {
        existe(EstadoNotificacion.FALLIDA);
        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", null)).hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", "  ")).hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", "laura-correo.com")).hasMessageContaining("correo");
        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", "a".repeat(115) + "@x.com"))
                .hasMessageContaining("correo");
        existe(EstadoNotificacion.FALLIDA).setCanal(Canal.SMS);
        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", "12345")).hasMessageContaining("celular");
        verify(solicitud, never()).solicitar(anyString());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoNotificacion.class, names = {"PENDIENTE", "ENVIADA", "LEIDA"})
    void soloSeCorrigenAvisosFallidos(EstadoNotificacion estado) {
        existe(estado);

        assertThatThrownBy(() -> corregir.ejecutar("NOT-2026-000001", "otro@correo.com"))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("NOTIFICACION_NO_FALLIDA");
    }
}
