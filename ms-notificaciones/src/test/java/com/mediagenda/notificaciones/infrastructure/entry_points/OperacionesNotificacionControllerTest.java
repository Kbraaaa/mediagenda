package com.mediagenda.notificaciones.infrastructure.entry_points;

import com.mediagenda.notificaciones.Datos;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.ConflictoException;
import com.mediagenda.notificaciones.domain.model.exception.NoEncontradoException;
import com.mediagenda.notificaciones.domain.usecase.CorregirDestinatarioUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReenviarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarFallidasUseCase;
import com.mediagenda.notificaciones.infrastructure.mapper.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperacionesNotificacionControllerTest {

    private static final String URL = "/api/v1/notificaciones";

    private ReenviarNotificacionUseCase reenviar;
    private CorregirDestinatarioUseCase corregir;
    private ReintentarFallidasUseCase reintentar;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        reenviar = mock(ReenviarNotificacionUseCase.class);
        corregir = mock(CorregirDestinatarioUseCase.class);
        reintentar = mock(ReintentarFallidasUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new OperacionesNotificacionController(reenviar, corregir, reintentar, new DtoMapper()))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void reenvioResponde202ConLaCopia() throws Exception {
        Notificacion copia = Datos.notificacion(EstadoNotificacion.PENDIENTE);
        copia.setCodigo("NOT-2026-000007");
        when(reenviar.ejecutar("NOT-2026-000001")).thenReturn(copia);

        mvc.perform(post(URL + "/NOT-2026-000001/reenvio"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codigo").value("NOT-2026-000007"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void reenvioDeAvisoFallidoResponde409() throws Exception {
        when(reenviar.ejecutar("NOT-2026-000001"))
                .thenThrow(new ConflictoException("NOTIFICACION_NO_ENTREGADA", "esta FALLIDA"));

        mvc.perform(post(URL + "/NOT-2026-000001/reenvio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOTIFICACION_NO_ENTREGADA"));
    }

    @Test
    void corregirDestinatarioResponde202() throws Exception {
        Notificacion n = Datos.notificacion(EstadoNotificacion.PENDIENTE);
        n.setDestinatario("laura.gomez@correo.com");
        when(corregir.ejecutar("NOT-2026-000001", "laura.gomez@correo.com")).thenReturn(n);

        mvc.perform(patch(URL + "/NOT-2026-000001/destinatario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"laura.gomez@correo.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.destinatario").value("laura.gomez@correo.com"));
    }

    @Test
    void corregirDestinatarioInvalidoOInexistente() throws Exception {
        when(corregir.ejecutar(eq("NOT-2026-000001"), isNull())).thenThrow(new IllegalArgumentException("obligatorio"));
        when(corregir.ejecutar("NOT-2026-999999", "x@correo.com"))
                .thenThrow(new NoEncontradoException("NOTIFICACION_NO_ENCONTRADA", "No existe"));

        mvc.perform(patch(URL + "/NOT-2026-000001/destinatario").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(URL + "/NOT-2026-999999/destinatario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"x@correo.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reintentoEnLoteDevuelveCuantasSeReintentaron() throws Exception {
        when(reintentar.ejecutar()).thenReturn(3);

        mvc.perform(post(URL + "/reintentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reintentadas").value(3));
    }
}
