package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.usecase.ReintentarTriageUseCase;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperacionesCitaControllerTest {

    private static final String URL = "/api/v1/citas/CIT-2026-000001/triage";

    private ReintentarTriageUseCase reintentar;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        reintentar = mock(ReintentarTriageUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperacionesCitaController(reintentar, new DtoMapper()))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void reintentarTriageResponde202ConElTriagePendiente() throws Exception {
        Cita cita = Datos.cita();
        when(reintentar.ejecutar("CIT-2026-000001")).thenReturn(cita);

        mvc.perform(post(URL))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codigo").value("CIT-2026-000001"))
                .andExpect(jsonPath("$.triage.estado").value("PENDIENTE"));
    }

    @Test
    void triageQueNoFalloResponde409() throws Exception {
        when(reintentar.ejecutar("CIT-2026-000001"))
                .thenThrow(new ConflictoException("TRIAGE_NO_FALLIDO", "esta COMPLETADO"));

        mvc.perform(post(URL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRIAGE_NO_FALLIDO"));
    }

    @Test
    void citaInexistenteResponde404() throws Exception {
        when(reintentar.ejecutar("CIT-2026-000001"))
                .thenThrow(new NoEncontradoException("CITA_NO_ENCONTRADA", "No existe la cita"));

        mvc.perform(post(URL)).andExpect(status().isNotFound());
    }
}
