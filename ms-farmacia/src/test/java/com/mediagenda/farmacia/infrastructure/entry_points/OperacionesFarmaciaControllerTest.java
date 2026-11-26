package com.mediagenda.farmacia.infrastructure.entry_points;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.BajaInventario;
import com.mediagenda.farmacia.domain.model.DisponibilidadFormula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.MotivoBaja;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.usecase.DarDeBajaMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.VerificarDisponibilidadUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperacionesFarmaciaControllerTest {

    private DarDeBajaMedicamentoUseCase baja;
    private VerificarDisponibilidadUseCase disponibilidad;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        baja = mock(DarDeBajaMedicamentoUseCase.class);
        disponibilidad = mock(VerificarDisponibilidadUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperacionesFarmaciaController(baja, disponibilidad))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void bajaResponde200ConElStockNuevo() throws Exception {
        Medicamento m = Datos.acetaminofen();
        m.setStock(480);
        when(baja.ejecutar("MDT-0001", 20, "VENCIMIENTO")).thenReturn(new BajaInventario(m, 20, MotivoBaja.VENCIMIENTO));

        mvc.perform(post("/api/v1/medicamentos/MDT-0001/bajas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":20,\"motivo\":\"VENCIMIENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDadaDeBaja").value(20))
                .andExpect(jsonPath("$.motivo").value("VENCIMIENTO"))
                .andExpect(jsonPath("$.stock").value(480));
    }

    @Test
    void bajaMayorQueElStockResponde409() throws Exception {
        when(baja.ejecutar("MDT-0001", 900, "DETERIORO"))
                .thenThrow(new ConflictoException("STOCK_INSUFICIENTE", "solo hay 500"));

        mvc.perform(post("/api/v1/medicamentos/MDT-0001/bajas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":900,\"motivo\":\"DETERIORO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
    }

    @Test
    void bajaSinCuerpoResponde400() throws Exception {
        mvc.perform(post("/api/v1/medicamentos/MDT-0001/bajas").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disponibilidadResponde200ConLosFaltantes() throws Exception {
        when(disponibilidad.ejecutar("FOR-2026-000001")).thenReturn(new DisponibilidadFormula("FOR-2026-000001", false,
                List.of(new DisponibilidadFormula.Item("MDT-0002", "Ibuprofeno", 5, 3, 2))));

        mvc.perform(get("/api/v1/formulas/FOR-2026-000001/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispensable").value(false))
                .andExpect(jsonPath("$.items[0].faltan").value(2));
    }

    @Test
    void disponibilidadDeFormulaInexistenteResponde404() throws Exception {
        when(disponibilidad.ejecutar("FOR-2026-999999"))
                .thenThrow(new NoEncontradoException("FORMULA_NO_ENCONTRADA", "No existe"));

        mvc.perform(get("/api/v1/formulas/FOR-2026-999999/disponibilidad")).andExpect(status().isNotFound());
    }
}
