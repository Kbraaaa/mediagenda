package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.Datos;
import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Pagina;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.exception.EnvioCorreoException;
import com.mediagenda.notificaciones.domain.model.exception.EnvioSmsException;
import com.mediagenda.notificaciones.domain.model.exception.NoEncontradoException;
import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionesUseCaseTest {

    private NotificacionGateway notificaciones;
    private CorreoGateway correo;
    private SmsGateway sms;
    private SolicitudEnvioGateway solicitud;
    private Despachador despachador;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionGateway.class);
        correo = mock(CorreoGateway.class);
        sms = mock(SmsGateway.class);
        solicitud = mock(SolicitudEnvioGateway.class);
        despachador = new Despachador(correo, sms, notificaciones, Datos.RELOJ);
        when(notificaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(notificaciones.siguienteConsecutivo()).thenReturn(7L);
    }

    // ---------- Eventos de RabbitMQ ----------

    @Test
    void eventoNuevoSeEnviaYQuedaEnviado() {
        Optional<Notificacion> n = new ProcesarEventoUseCase(notificaciones, despachador)
                .ejecutar(Datos.eventoCita("cita.agendada"));

        assertThat(n).isPresent();
        assertThat(n.get().getCodigo()).isEqualTo("NOT-2026-000007");
        assertThat(n.get().getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(n.get().getIntentos()).isEqualTo(1);
        assertThat(n.get().getFechaEnvio()).isNotNull();
        verify(correo).enviar("laura@correo.com", "Cita agendada CIT-2026-000001", n.get().getCuerpo());
    }

    @Test
    void eventoRepetidoSeIgnora() {
        when(notificaciones.existeEvento("evt-1")).thenReturn(true);

        assertThat(new ProcesarEventoUseCase(notificaciones, despachador).ejecutar(Datos.eventoCita("cita.agendada")))
                .isEmpty();
        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void siElCorreoFallaQuedaFallidaConElError() {
        doThrow(new EnvioCorreoException("SMTP caido", null)).when(correo).enviar(anyString(), anyString(), anyString());

        Notificacion n = new ProcesarEventoUseCase(notificaciones, despachador)
                .ejecutar(Datos.eventoFormula("formula.emitida")).orElseThrow();

        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(n.getUltimoError()).isEqualTo("SMTP caido");
    }

    @Test
    void errorLargoOSinMensajeSeGuardaRecortado() {
        doThrow(new EnvioCorreoException("x".repeat(600), null))
                .doThrow(new EnvioCorreoException(null, null))
                .when(correo).enviar(anyString(), anyString(), anyString());

        assertThat(despachador.despachar(Datos.notificacion(EstadoNotificacion.PENDIENTE)).getUltimoError()).hasSize(500);
        assertThat(despachador.despachar(Datos.notificacion(EstadoNotificacion.PENDIENTE)).getUltimoError())
                .isEqualTo("Error desconocido");
    }

    @Test
    void eventosInvalidos() {
        ProcesarEventoUseCase useCase = new ProcesarEventoUseCase(notificaciones, despachador);
        EventoRecibido sinId = new EventoRecibido(" ", "t", null, null, "a@b.co", null, null, null, null, null, null, null, null, null);
        EventoRecibido idNulo = new EventoRecibido(null, "t", null, null, "a@b.co", null, null, null, null, null, null, null, null, null);
        EventoRecibido sinCorreo = new EventoRecibido("e", "t", null, null, " ", null, null, null, null, null, null, null, null, null);
        EventoRecibido correoNulo = new EventoRecibido("e", "t", null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> useCase.ejecutar(sinId)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(idNulo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(sinCorreo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(correoNulo)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eventoSinTipoYTextosLargos() {
        EventoRecibido sinTipo = new EventoRecibido("e-9", null, null, null, "a@b.co", null, null, null, null, null, null,
                null, null, "y".repeat(2500));

        Notificacion n = new ProcesarEventoUseCase(notificaciones, despachador).ejecutar(sinTipo).orElseThrow();

        assertThat(n.getTipo()).isEqualTo("desconocido");
        assertThat(n.getCuerpo()).hasSize(2000);
        assertThat(ProcesarEventoUseCase.recortar("corto", 10)).isEqualTo("corto");
    }

    // ---------- SMS ----------

    @Test
    void conSmsHabilitadoSeEnviaCorreoYSms() {
        ArgumentCaptor<Notificacion> guardadas = ArgumentCaptor.forClass(Notificacion.class);

        Notificacion n = new ProcesarEventoUseCase(notificaciones, despachador, true)
                .ejecutar(Datos.eventoCita("cita.agendada")).orElseThrow();

        assertThat(n.getCanal()).isEqualTo(Canal.CORREO);
        verify(sms).enviar(eq("+573101234567"), contains("CIT-2026-000001"));
        verify(notificaciones, atLeastOnce()).guardar(guardadas.capture());
        Notificacion delSms = guardadas.getAllValues().stream().filter(x -> x.getCanal() == Canal.SMS).findFirst().orElseThrow();
        assertThat(delSms.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(delSms.getEventoId()).isEqualTo("evt-1");
        assertThat(delSms.getAsunto()).startsWith("SMS: ");
    }

    @Test
    void sinSmsHabilitadoSinCelularOSinPlantillaNoSeEnviaSms() {
        new ProcesarEventoUseCase(notificaciones, despachador).ejecutar(Datos.eventoCita("cita.agendada"));
        EventoRecibido sinCelular = new EventoRecibido("e-1", "cita.cancelada", "P", "N", "a@b.co", "12345",
                "C", "M", "E", "2026-11-21", "08:00:00", null, null, null);
        new ProcesarEventoUseCase(notificaciones, despachador, true).ejecutar(sinCelular);
        new ProcesarEventoUseCase(notificaciones, despachador, true).ejecutar(Datos.eventoFormula("formula.dispensada"));

        verify(sms, never()).enviar(anyString(), anyString());
        verify(correo, times(3)).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void siSnsRechazaElSmsQuedaFallidoSinAfectarElCorreo() {
        doThrow(new EnvioSmsException("numero no verificado", null)).when(sms).enviar(anyString(), anyString());
        Notificacion delSms = Datos.notificacion(EstadoNotificacion.PENDIENTE).toBuilder()
                .canal(Canal.SMS).destinatario("+573101234567").build();

        assertThat(despachador.despachar(delSms).getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(delSms.getUltimoError()).isEqualTo("numero no verificado");
        Notificacion n = new ProcesarEventoUseCase(notificaciones, despachador, true)
                .ejecutar(Datos.eventoCita("cita.reprogramada")).orElseThrow();
        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    // ---------- APIs ----------

    @Test
    void avisoManualSeGuardaPendienteYSePideElEnvio() {
        Notificacion n = new CrearNotificacionManualUseCase(notificaciones, solicitud, despachador)
                .ejecutar(" laura@correo.com ", " Asunto ", " Cuerpo ", " PAC-000001 ");

        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(n.getTipo()).isEqualTo(Notificacion.TIPO_MANUAL);
        assertThat(n.getDestinatario()).isEqualTo("laura@correo.com");
        assertThat(n.getCodigoPaciente()).isEqualTo("PAC-000001");
        verify(solicitud).solicitar("NOT-2026-000007");
        assertThat(new CrearNotificacionManualUseCase(notificaciones, solicitud, despachador)
                .ejecutar("a@b.co", "A", "B", null).getCodigoPaciente()).isNull();
        assertThat(new CrearNotificacionManualUseCase(notificaciones, solicitud, despachador)
                .ejecutar("a@b.co", "A", "B", " ").getCodigoPaciente()).isNull();
    }

    @Test
    void avisoManualValida() {
        CrearNotificacionManualUseCase useCase = new CrearNotificacionManualUseCase(notificaciones, solicitud, despachador);

        assertThatThrownBy(() -> useCase.ejecutar(null, "a", "b", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("sin-arroba", "a", "b", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("a@b.co", " ", "b", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("asunto");
        assertThatThrownBy(() -> useCase.ejecutar("a@b.co", null, "b", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("a@b.co", "a", "x".repeat(2001), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2000");
        verify(notificaciones, never()).guardar(any());
    }

    @Test
    void consultarYListar() {
        when(notificaciones.buscarPorCodigo("NOT-2026-000001"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.ENVIADA)));
        when(notificaciones.buscarPorCodigo("NOT-404")).thenReturn(Optional.empty());
        FiltroNotificaciones filtro = new FiltroNotificaciones("PAC-000001", null);
        Pagina<Notificacion> pagina = new Pagina<>(List.of(), 0, 10, 0, 0);
        when(notificaciones.listar(filtro, 0, 10)).thenReturn(pagina);

        assertThat(new ConsultarNotificacionUseCase(notificaciones).ejecutar("NOT-2026-000001").getCodigo())
                .isEqualTo("NOT-2026-000001");
        assertThatThrownBy(() -> new ConsultarNotificacionUseCase(notificaciones).ejecutar("NOT-404"))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(new ListarNotificacionesUseCase(notificaciones).ejecutar(filtro, 0, 10)).isSameAs(pagina);
    }

    @Test
    void reintentarSoloFallidas() {
        when(notificaciones.buscarPorCodigo("NOT-F"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.FALLIDA)));
        when(notificaciones.buscarPorCodigo("NOT-E"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.ENVIADA)));
        ReintentarNotificacionUseCase useCase = new ReintentarNotificacionUseCase(notificaciones, solicitud);

        assertThat(useCase.ejecutar("NOT-F").getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        verify(solicitud).solicitar("NOT-F");
        assertThatThrownBy(() -> useCase.ejecutar("NOT-E"))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("NOTIFICACION_NO_FALLIDA");
    }

    @Test
    void marcarLeida() {
        when(notificaciones.buscarPorCodigo("NOT-E"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.ENVIADA)));
        when(notificaciones.buscarPorCodigo("NOT-L"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.LEIDA)));
        when(notificaciones.buscarPorCodigo("NOT-P"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.PENDIENTE)));
        MarcarLeidaUseCase useCase = new MarcarLeidaUseCase(notificaciones);

        assertThat(useCase.ejecutar("NOT-E").getEstado()).isEqualTo(EstadoNotificacion.LEIDA);
        assertThat(useCase.ejecutar("NOT-L").getEstado()).isEqualTo(EstadoNotificacion.LEIDA);
        verify(notificaciones, times(1)).guardar(any());
        assertThatThrownBy(() -> useCase.ejecutar("NOT-P"))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("NOTIFICACION_NO_ENVIADA");
    }

    // ---------- Envio en segundo plano y reintentos automaticos ----------

    @Test
    void enviarPendienteSoloSiSiguePendiente() {
        when(notificaciones.buscarPorCodigo("NOT-P"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.PENDIENTE)));
        when(notificaciones.buscarPorCodigo("NOT-E"))
                .thenReturn(Optional.of(Datos.notificacion(EstadoNotificacion.ENVIADA)));
        EnviarPendienteUseCase useCase = new EnviarPendienteUseCase(notificaciones, despachador);

        useCase.ejecutar("NOT-P");
        useCase.ejecutar("NOT-E");
        useCase.ejecutar("NOT-404");

        verify(correo, times(1)).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void reintentaLasFallidas() {
        when(notificaciones.fallidasParaReintento(3)).thenReturn(List.of(
                Datos.notificacion(EstadoNotificacion.FALLIDA), Datos.notificacion(EstadoNotificacion.FALLIDA)));

        int reintentadas = new ReintentarFallidasUseCase(notificaciones, despachador, 3).ejecutar();

        assertThat(reintentadas).isEqualTo(2);
        verify(correo, times(2)).enviar(anyString(), anyString(), anyString());
    }
}
