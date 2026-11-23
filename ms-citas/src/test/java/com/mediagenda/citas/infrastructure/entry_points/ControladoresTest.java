package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.DiaFestivoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.exception.ServicioNoDisponibleException;
import com.mediagenda.citas.domain.usecase.AbrirAgendaUseCase;
import com.mediagenda.citas.domain.usecase.AgendarCitaUseCase;
import com.mediagenda.citas.domain.usecase.AtenderCitaUseCase;
import com.mediagenda.citas.domain.usecase.CancelarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarMedicoUseCase;
import com.mediagenda.citas.domain.usecase.ListarCitasUseCase;
import com.mediagenda.citas.domain.usecase.ListarFranjasUseCase;
import com.mediagenda.citas.domain.usecase.ListarMedicosUseCase;
import com.mediagenda.citas.domain.usecase.RegistrarMedicoUseCase;
import com.mediagenda.citas.domain.usecase.ReprogramarCitaUseCase;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las APIs de citas, medicos y franjas, y todos los codigos de error, con MockMvc. */
class ControladoresTest {

    private static final String AGENDAR = """
            {"codigoPaciente":"PAC-000001","idFranja":10,"sintomas":"Dolor de cabeza fuerte desde hace dos dias"}""";

    private AgendarCitaUseCase agendar;
    private ConsultarCitaUseCase consultar;
    private ListarCitasUseCase listar;
    private ReprogramarCitaUseCase reprogramar;
    private CancelarCitaUseCase cancelar;
    private AtenderCitaUseCase atender;
    private RegistrarMedicoUseCase registrarMedico;
    private ConsultarMedicoUseCase consultarMedico;
    private ListarMedicosUseCase listarMedicos;
    private AbrirAgendaUseCase abrirAgenda;
    private ListarFranjasUseCase listarFranjas;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        agendar = mock(AgendarCitaUseCase.class);
        consultar = mock(ConsultarCitaUseCase.class);
        listar = mock(ListarCitasUseCase.class);
        reprogramar = mock(ReprogramarCitaUseCase.class);
        cancelar = mock(CancelarCitaUseCase.class);
        atender = mock(AtenderCitaUseCase.class);
        registrarMedico = mock(RegistrarMedicoUseCase.class);
        consultarMedico = mock(ConsultarMedicoUseCase.class);
        listarMedicos = mock(ListarMedicosUseCase.class);
        abrirAgenda = mock(AbrirAgendaUseCase.class);
        listarFranjas = mock(ListarFranjasUseCase.class);
        DtoMapper mapper = new DtoMapper();
        mvc = MockMvcBuilders.standaloneSetup(
                        new CitaController(agendar, consultar, listar, reprogramar, cancelar, atender, mapper),
                        new MedicoController(registrarMedico, consultarMedico, listarMedicos, abrirAgenda, mapper),
                        new FranjaController(listarFranjas, mapper))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    private static Cita citaConTriage() {
        Cita cita = Datos.cita();
        cita.setEstadoTriage(EstadoTriage.COMPLETADO);
        cita.setEspecialidadSugerida(Especialidad.NEUROLOGIA);
        cita.setPrioridad(Prioridad.MEDIA);
        cita.setMotivoTriage("cefalea");
        return cita;
    }

    // ---------- Citas ----------

    @Test
    void agendarResponde201ConTriagePendiente() throws Exception {
        when(agendar.ejecutar("PAC-000001", 10L, "Dolor de cabeza fuerte desde hace dos dias")).thenReturn(Datos.cita());

        mvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON).content(AGENDAR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CIT-2026-000001"))
                .andExpect(jsonPath("$.franja.idFranja").value(10))
                .andExpect(jsonPath("$.franja.codigoMedico").value("MED-0001"))
                .andExpect(jsonPath("$.triage.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.triage.prioridad").doesNotExist());
    }

    @Test
    void consultarMuestraElTriage() throws Exception {
        when(consultar.ejecutar("CIT-2026-000001")).thenReturn(citaConTriage());

        mvc.perform(get("/api/v1/citas/CIT-2026-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triage.especialidadSugerida").value("NEUROLOGIA"))
                .andExpect(jsonPath("$.triage.prioridad").value("MEDIA"));
    }

    @Test
    void listarConFiltros() throws Exception {
        when(listar.ejecutar(new FiltroCitas("PAC-000001", null, EstadoCita.AGENDADA), 0, 5))
                .thenReturn(new Pagina<>(List.of(Datos.cita()), 0, 5, 1, 1));

        mvc.perform(get("/api/v1/citas").param("paciente", "PAC-000001").param("medico", " ")
                        .param("estado", "agendada").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].codigo").value("CIT-2026-000001"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void reprogramarCancelarYAtender() throws Exception {
        when(reprogramar.ejecutar("CIT-2026-000001", 11L)).thenReturn(Datos.cita());
        when(cancelar.ejecutar("CIT-2026-000001")).thenReturn(Datos.cita());
        when(atender.ejecutar("CIT-2026-000001")).thenReturn(Datos.cita());

        mvc.perform(put("/api/v1/citas/CIT-2026-000001/reprogramacion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idFranja\":11}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/citas/CIT-2026-000001/cancelacion")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/citas/CIT-2026-000001/atencion")).andExpect(status().isOk());
    }

    // ---------- Medicos y franjas ----------

    @Test
    void registrarYConsultarMedico() throws Exception {
        when(registrarMedico.ejecutar(any())).thenReturn(Datos.medico());
        when(consultarMedico.ejecutar("MED-0001")).thenReturn(Datos.medico());

        mvc.perform(post("/api/v1/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Andrea Lopez\",\"especialidad\":\"medicina general\",\"correo\":\"a@b.co\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MED-0001"));
        mvc.perform(get("/api/v1/medicos/MED-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("MEDICINA_GENERAL"));
    }

    @Test
    void listarMedicosYAbrirAgenda() throws Exception {
        when(listarMedicos.ejecutar(Especialidad.CARDIOLOGIA, 0, 10)).thenReturn(new Pagina<>(List.of(), 0, 10, 0, 0));
        when(listarMedicos.ejecutar(null, 0, 10)).thenReturn(new Pagina<>(List.of(Datos.medico()), 0, 10, 1, 1));
        when(abrirAgenda.ejecutar(eq("MED-0001"), any())).thenReturn(List.of(Datos.franjaManana()));

        mvc.perform(get("/api/v1/medicos").param("especialidad", "cardiologia"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isEmpty());
        mvc.perform(get("/api/v1/medicos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Andrea Lopez"));
        mvc.perform(post("/api/v1/medicos/MED-0001/franjas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-11-21\",\"horaInicio\":\"08:00\",\"horaFin\":\"08:30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
    }

    @Test
    void listarFranjas() throws Exception {
        when(listarFranjas.ejecutar(new FiltroFranjas("MED-0001", LocalDate.of(2026, 11, 21), true), 0, 20))
                .thenReturn(new Pagina<>(List.of(Datos.franjaManana()), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/franjas").param("medico", "MED-0001").param("fecha", "2026-11-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idFranja").value(10));
    }

    // ---------- Errores ----------

    @Test
    void codigosDeError() throws Exception {
        when(consultar.ejecutar("CIT-404")).thenThrow(new NoEncontradoException("CITA_NO_ENCONTRADA", "no existe"));
        when(cancelar.ejecutar("CIT-409")).thenThrow(new ConflictoException("CITA_NO_AGENDADA", "cancelada"));
        when(cancelar.ejecutar("CIT-DB")).thenThrow(new DataIntegrityViolationException("uq"));
        when(atender.ejecutar("CIT-500")).thenThrow(new IllegalStateException("interno"));
        when(agendar.ejecutar(eq("PAC-FESTIVO"), anyLong(), anyString()))
                .thenThrow(new DiaFestivoException(LocalDate.of(2026, 12, 8)));
        when(agendar.ejecutar(eq("PAC-CAIDO"), anyLong(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("ms-pacientes no responde", null));

        mvc.perform(get("/api/v1/citas/CIT-404")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CITA_NO_ENCONTRADA"));
        mvc.perform(patch("/api/v1/citas/CIT-409/cancelacion")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CITA_NO_AGENDADA"));
        mvc.perform(patch("/api/v1/citas/CIT-DB/cancelacion")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_DATOS"));
        mvc.perform(patch("/api/v1/citas/CIT-500/atencion")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));
        mvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDAR.replace("PAC-000001", "PAC-FESTIVO")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("DIA_FESTIVO"));
        mvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(AGENDAR.replace("PAC-000001", "PAC-CAIDO")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));
    }

    @Test
    void cuerposNulosLlamadosDirectamente() {
        CitaController citas = new CitaController(agendar, consultar, listar, reprogramar, cancelar, atender,
                new DtoMapper());
        when(reprogramar.ejecutar("CIT-1", null)).thenThrow(new IllegalArgumentException("idFranja obligatorio"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> citas.agendar(null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> citas.reprogramar("CIT-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void peticionesInvalidasResponden400() throws Exception {
        mvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON).content("{ no es json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
        mvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/citas").param("estado", "PERDIDA"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("AGENDADA")));
        mvc.perform(get("/api/v1/citas").param("page", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/medicos").param("especialidad", "astrologia")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/medicos").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/medicos/MED-0001/franjas").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/citas/CIT-1/reprogramacion").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
    }
}
