package com.mediagenda.notificaciones.infrastructure.entry_points;

import com.mediagenda.notificaciones.Datos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Pagina;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.exception.NoEncontradoException;
import com.mediagenda.notificaciones.domain.usecase.ConsultarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.CrearNotificacionManualUseCase;
import com.mediagenda.notificaciones.domain.usecase.ListarNotificacionesUseCase;
import com.mediagenda.notificaciones.domain.usecase.MarcarLeidaUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarNotificacionUseCase;
import com.mediagenda.notificaciones.infrastructure.mapper.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificacionControllerTest {

    private CrearNotificacionManualUseCase crear;
    private ConsultarNotificacionUseCase consultar;
    private ListarNotificacionesUseCase listar;
    private ReintentarNotificacionUseCase reintentar;
    private MarcarLeidaUseCase marcar;
    private NotificacionController controller;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        crear = mock(CrearNotificacionManualUseCase.class);
        consultar = mock(ConsultarNotificacionUseCase.class);
        listar = mock(ListarNotificacionesUseCase.class);
        reintentar = mock(ReintentarNotificacionUseCase.class);
        marcar = mock(MarcarLeidaUseCase.class);
        controller = new NotificacionController(crear, consultar, listar, reintentar, marcar, new DtoMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ManejadorExcepciones()).build();
    }

    @Test
    void lasCincoApis() throws Exception {
        when(crear.ejecutar("laura@correo.com", "Hola", "Mensaje", null))
                .thenReturn(Datos.notificacion(EstadoNotificacion.PENDIENTE));
        when(consultar.ejecutar("NOT-2026-000001")).thenReturn(Datos.notificacion(EstadoNotificacion.ENVIADA));
        when(listar.ejecutar(new FiltroNotificaciones("PAC-000001", EstadoNotificacion.FALLIDA), 0, 10))
                .thenReturn(new Pagina<>(List.of(Datos.notificacion(EstadoNotificacion.FALLIDA)), 0, 10, 1, 1));
        when(reintentar.ejecutar("NOT-2026-000001")).thenReturn(Datos.notificacion(EstadoNotificacion.PENDIENTE));
        when(marcar.ejecutar("NOT-2026-000001")).thenReturn(Datos.notificacion(EstadoNotificacion.LEIDA));

        mvc.perform(post("/api/v1/notificaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"laura@correo.com\",\"asunto\":\"Hola\",\"cuerpo\":\"Mensaje\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(get("/api/v1/notificaciones/NOT-2026-000001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.destinatario").value("laura@correo.com"))
                .andExpect(jsonPath("$.canal").value("CORREO"));
        mvc.perform(get("/api/v1/notificaciones").param("paciente", "PAC-000001").param("estado", "fallida"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].estado").value("FALLIDA"));
        mvc.perform(post("/api/v1/notificaciones/NOT-2026-000001/reintento")).andExpect(status().isAccepted());
        mvc.perform(patch("/api/v1/notificaciones/NOT-2026-000001/lectura"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("LEIDA"));
    }

    @Test
    void errores() throws Exception {
        when(consultar.ejecutar("NOT-404")).thenThrow(new NoEncontradoException("NOTIFICACION_NO_ENCONTRADA", "x"));
        when(reintentar.ejecutar("NOT-409")).thenThrow(new ConflictoException("NOTIFICACION_NO_FALLIDA", "x"));
        when(marcar.ejecutar("NOT-DB")).thenThrow(new DataIntegrityViolationException("x"));
        when(marcar.ejecutar("NOT-500")).thenThrow(new IllegalStateException("x"));

        mvc.perform(get("/api/v1/notificaciones/NOT-404")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notificaciones/NOT-409/reintento")).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/notificaciones/NOT-DB/lectura")).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/notificaciones/NOT-500/lectura")).andExpect(status().isInternalServerError());
        mvc.perform(post("/api/v1/notificaciones").contentType(MediaType.APPLICATION_JSON).content("{ mal"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notificaciones").param("estado", "perdida")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notificaciones").param("page", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void cuerpoNuloYFiltros() {
        assertThatThrownBy(() -> controller.enviar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(DtoMapper.textoOpcional(" ")).isNull();
        assertThat(DtoMapper.estadoOpcional(null)).isNull();
    }
}
