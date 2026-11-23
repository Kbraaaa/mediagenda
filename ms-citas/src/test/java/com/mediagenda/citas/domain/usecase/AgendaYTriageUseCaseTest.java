package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.SolicitudFranjas;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.DiaFestivoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.exception.TriageException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Medicos, apertura de agenda, triage con IA y recordatorios. */
class AgendaYTriageUseCaseTest {

    private MedicoGateway medicos;
    private FranjaGateway franjas;
    private CitaGateway citas;
    private EventoGateway eventos;
    private FestivoGateway festivos;
    private TriageGateway ia;
    private ReglasAgenda reglas;

    @BeforeEach
    void preparar() {
        medicos = mock(MedicoGateway.class);
        franjas = mock(FranjaGateway.class);
        citas = mock(CitaGateway.class);
        eventos = mock(EventoGateway.class);
        festivos = mock(FestivoGateway.class);
        ia = mock(TriageGateway.class);
        reglas = new ReglasAgenda(Datos.RELOJ, festivos);
        when(medicos.buscarPorCodigo("MED-0001")).thenReturn(Optional.of(Datos.medico()));
        when(medicos.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(franjas.guardarTodas(anyList())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- Medicos ----------

    @Test
    void registraMedicoConCodigoYCorreoNormalizado() {
        when(medicos.siguienteConsecutivo()).thenReturn(3L);
        Medico datos = Medico.builder().nombre("  Carlos Ruiz ").especialidad(Especialidad.CARDIOLOGIA)
                .correo(" Carlos.Ruiz@MediAgenda.co ").build();

        Medico medico = new RegistrarMedicoUseCase(medicos).ejecutar(datos);

        assertThat(medico.getCodigo()).isEqualTo("MED-0003");
        assertThat(medico.getNombre()).isEqualTo("Carlos Ruiz");
        assertThat(medico.getCorreo()).isEqualTo("carlos.ruiz@mediagenda.co");
        assertThat(medico.isActivo()).isTrue();
        assertThat(medico.getIdMedico()).isNotNull();
    }

    @Test
    void registrarMedicoValida() {
        RegistrarMedicoUseCase useCase = new RegistrarMedicoUseCase(medicos);
        Medico base = Medico.builder().nombre("Carlos Ruiz").especialidad(Especialidad.CARDIOLOGIA)
                .correo("carlos@mediagenda.co").build();
        when(medicos.existeCorreo("repetido@mediagenda.co")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().nombre("C").build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().especialidad(null).build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("especialidad");
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().correo("sin-arroba").build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("formato");
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().correo("repetido@mediagenda.co").build()))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("MEDICO_DUPLICADO");
    }

    @Test
    void consultaYListaMedicos() {
        Pagina<Medico> pagina = new Pagina<>(List.of(Datos.medico()), 0, 10, 1, 1);
        when(medicos.listar(Especialidad.CARDIOLOGIA, 0, 10)).thenReturn(pagina);
        when(medicos.buscarPorCodigo("MED-9999")).thenReturn(Optional.empty());

        assertThat(new ConsultarMedicoUseCase(medicos).ejecutar("MED-0001").getNombre()).isEqualTo("Andrea Lopez");
        assertThatThrownBy(() -> new ConsultarMedicoUseCase(medicos).ejecutar("MED-9999"))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(new ListarMedicosUseCase(medicos).ejecutar(Especialidad.CARDIOLOGIA, 0, 10)).isSameAs(pagina);
    }

    // ---------- Apertura de agenda ----------

    private AbrirAgendaUseCase abrirAgenda() {
        return new AbrirAgendaUseCase(medicos, franjas, reglas);
    }

    private static SolicitudFranjas solicitud(LocalDate fecha, String inicio, String fin, int minutos) {
        return new SolicitudFranjas(fecha, LocalTime.parse(inicio), LocalTime.parse(fin), minutos);
    }

    @Test
    void abreAgendaEnBloquesDeTreintaMinutos() {
        List<Franja> creadas = abrirAgenda().ejecutar("MED-0001", solicitud(Datos.MANANA, "08:00", "10:15", 30));

        assertThat(creadas).hasSize(4);
        assertThat(creadas.get(0).getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(creadas.get(3).getHoraFin()).isEqualTo(LocalTime.of(10, 0));
        verify(franjas).existeSolapamiento(any(), any(), any(), any());
    }

    @Test
    void abrirAgendaLimitaLaCantidadDeFranjas() {
        List<Franja> creadas = abrirAgenda().ejecutar("MED-0001", solicitud(Datos.MANANA, "00:00", "23:50", 10));

        assertThat(creadas).hasSize(AbrirAgendaUseCase.MAXIMO_FRANJAS);
    }

    @Test
    void abrirAgendaValidaLaSolicitud() {
        AbrirAgendaUseCase useCase = abrirAgenda();

        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", new SolicitudFranjas(null, LocalTime.NOON, LocalTime.MIDNIGHT, 30)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", new SolicitudFranjas(Datos.MANANA, null, LocalTime.NOON, 30)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", new SolicitudFranjas(Datos.MANANA, LocalTime.NOON, null, 30)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.MANANA, "10:00", "09:00", 30)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("posterior");
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.MANANA, "08:00", "12:00", 5)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duracion");
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.MANANA, "08:00", "12:00", 200)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.MANANA, "08:00", "08:20", 30)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no alcanza");
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.HOY.minusDays(1), "08:00", "12:00", 30)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pasada");
    }

    @Test
    void abrirAgendaReglasDeNegocio() {
        AbrirAgendaUseCase useCase = abrirAgenda();
        Medico inactivo = Datos.medico().toBuilder().codigo("MED-0002").activo(false).build();
        when(medicos.buscarPorCodigo("MED-0002")).thenReturn(Optional.of(inactivo));
        when(medicos.buscarPorCodigo("MED-9999")).thenReturn(Optional.empty());
        LocalDate festivo = LocalDate.of(2026, 12, 8);
        when(festivos.esFestivo(festivo)).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar("MED-9999", solicitud(Datos.MANANA, "08:00", "12:00", 30)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0002", solicitud(Datos.MANANA, "08:00", "12:00", 30)))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("MEDICO_INACTIVO");
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(festivo, "08:00", "12:00", 30)))
                .isInstanceOf(DiaFestivoException.class);

        when(franjas.existeSolapamiento(any(), any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> useCase.ejecutar("MED-0001", solicitud(Datos.MANANA, "08:00", "12:00", 30)))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("AGENDA_SOLAPADA");
        verify(franjas, never()).guardarTodas(anyList());
    }

    @Test
    void listaFranjas() {
        FiltroFranjas filtro = new FiltroFranjas("MED-0001", Datos.MANANA, true);
        Pagina<Franja> pagina = new Pagina<>(List.of(Datos.franjaManana()), 0, 20, 1, 1);
        when(franjas.listar(filtro, 0, 20)).thenReturn(pagina);

        assertThat(new ListarFranjasUseCase(franjas).ejecutar(filtro, 0, 20)).isSameAs(pagina);
    }

    // ---------- Triage ----------

    private ProcesarTriageUseCase procesarTriage() {
        return new ProcesarTriageUseCase(citas, ia, eventos, 3);
    }

    @Test
    void triageExitosoConPrioridadAltaYOtraEspecialidad() {
        when(citas.buscarPorCodigo("CIT-2026-000001")).thenReturn(Optional.of(Datos.cita()));
        ResultadoTriage resultado = new ResultadoTriage(Especialidad.CARDIOLOGIA, Prioridad.ALTA, "dolor toracico");
        when(ia.analizar(any())).thenReturn(resultado);

        procesarTriage().ejecutar("CIT-2026-000001");

        verify(citas).actualizarTriage("CIT-2026-000001", EstadoTriage.COMPLETADO, resultado);
        ArgumentCaptor<EventoCita> evento = ArgumentCaptor.forClass(EventoCita.class);
        verify(eventos).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(EventoCita.TRIAGE_COMPLETADO);
        assertThat(evento.getValue().detalle())
                .contains("urgencias").contains("CARDIOLOGIA").contains("no es un diagnostico");
    }

    @Test
    void recomendacionCuandoLaEspecialidadCoincide() {
        String texto = ProcesarTriageUseCase.recomendacion(Datos.cita(),
                new ResultadoTriage(Especialidad.MEDICINA_GENERAL, Prioridad.BAJA, "leve"));

        assertThat(texto).contains("adecuada").doesNotContain("urgencias");
    }

    @Test
    void triageReintentaYSiSigueFallandoQuedaFallido() {
        when(citas.buscarPorCodigo("CIT-2026-000001")).thenReturn(Optional.of(Datos.cita()));
        when(ia.analizar(any())).thenThrow(new TriageException("sin respuesta", null));

        procesarTriage().ejecutar("CIT-2026-000001");

        verify(ia, times(3)).analizar(any());
        verify(citas).actualizarTriage("CIT-2026-000001", EstadoTriage.FALLIDO, null);
        verify(eventos, never()).publicar(any());
    }

    @Test
    void triageFallaUnaVezYLuegoResponde() {
        when(citas.buscarPorCodigo("CIT-2026-000001")).thenReturn(Optional.of(Datos.cita()));
        ResultadoTriage resultado = new ResultadoTriage(Especialidad.NEUROLOGIA, Prioridad.MEDIA, "cefalea");
        when(ia.analizar(any())).thenThrow(new TriageException("429", null)).thenReturn(resultado);

        procesarTriage().ejecutar("CIT-2026-000001");

        verify(ia, times(2)).analizar(any());
        verify(citas).actualizarTriage("CIT-2026-000001", EstadoTriage.COMPLETADO, resultado);
    }

    @Test
    void triageIgnoraCitasInexistentesOYaProcesadas() {
        Cita procesada = Datos.cita();
        procesada.setEstadoTriage(EstadoTriage.COMPLETADO);
        when(citas.buscarPorCodigo("CIT-2026-000002")).thenReturn(Optional.of(procesada));
        when(citas.buscarPorCodigo("CIT-2026-000404")).thenReturn(Optional.empty());

        procesarTriage().ejecutar("CIT-2026-000002");
        procesarTriage().ejecutar("CIT-2026-000404");

        verify(ia, never()).analizar(any());
    }

    // ---------- Recordatorios ----------

    @Test
    void publicaRecordatoriosDeLasProximas24Horas() {
        Cita cita = Datos.cita();
        when(citas.pendientesDeRecordatorio(any(), any())).thenReturn(List.of(cita));

        int enviados = new EnviarRecordatoriosUseCase(citas, eventos, reglas).ejecutar();

        assertThat(enviados).isEqualTo(1);
        verify(citas).pendientesDeRecordatorio(reglas.ahora(), reglas.ahora().plusHours(24));
        verify(citas).marcarRecordatorioEnviado("CIT-2026-000001");
        verify(eventos).publicar(any());
    }
}
