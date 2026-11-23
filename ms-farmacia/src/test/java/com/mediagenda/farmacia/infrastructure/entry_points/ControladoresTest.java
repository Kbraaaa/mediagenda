package com.mediagenda.farmacia.infrastructure.entry_points;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.SolicitudItem;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.exception.ServicioNoDisponibleException;
import com.mediagenda.farmacia.domain.usecase.AnularFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ConsultarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ConsultarMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.DispensarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.EmitirFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarFormulasUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarMedicamentosUseCase;
import com.mediagenda.farmacia.domain.usecase.ReabastecerMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.RegistrarMedicamentoUseCase;
import com.mediagenda.farmacia.infrastructure.mapper.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladoresTest {

    private static final String EMITIR = """
            {"codigoCita":"CIT-2026-000001","items":[
              {"codigoMedicamento":"MDT-0001","cantidad":20,"indicaciones":"1 cada 8 horas"},
              {"codigoMedicamento":"MDT-0002","cantidad":2}]}""";

    private EmitirFormulaUseCase emitir;
    private ConsultarFormulaUseCase consultar;
    private ListarFormulasUseCase listar;
    private DispensarFormulaUseCase dispensar;
    private AnularFormulaUseCase anular;
    private RegistrarMedicamentoUseCase registrar;
    private ConsultarMedicamentoUseCase consultarMed;
    private ListarMedicamentosUseCase listarMed;
    private ReabastecerMedicamentoUseCase reabastecer;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        emitir = mock(EmitirFormulaUseCase.class);
        consultar = mock(ConsultarFormulaUseCase.class);
        listar = mock(ListarFormulasUseCase.class);
        dispensar = mock(DispensarFormulaUseCase.class);
        anular = mock(AnularFormulaUseCase.class);
        registrar = mock(RegistrarMedicamentoUseCase.class);
        consultarMed = mock(ConsultarMedicamentoUseCase.class);
        listarMed = mock(ListarMedicamentosUseCase.class);
        reabastecer = mock(ReabastecerMedicamentoUseCase.class);
        DtoMapper mapper = new DtoMapper();
        mvc = MockMvcBuilders.standaloneSetup(
                        new FormulaController(emitir, consultar, listar, dispensar, anular, mapper),
                        new MedicamentoController(registrar, consultarMed, listarMed, reabastecer, mapper))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void lasCincoApisDeFormulas() throws Exception {
        Formula dispensada = Datos.formula();
        dispensada.setEstado(EstadoFormula.DISPENSADA);
        when(emitir.ejecutar(eq("CIT-2026-000001"), anyList())).thenReturn(Datos.formula());
        when(consultar.ejecutar("FOR-2026-000001")).thenReturn(Datos.formula());
        when(listar.ejecutar(new FiltroFormulas("PAC-000001", EstadoFormula.EMITIDA), 0, 10))
                .thenReturn(new Pagina<>(List.of(Datos.formula()), 0, 10, 1, 1));
        when(dispensar.ejecutar("FOR-2026-000001")).thenReturn(dispensada);
        when(anular.ejecutar("FOR-2026-000001")).thenReturn(Datos.formula());

        mvc.perform(post("/api/v1/formulas").contentType(MediaType.APPLICATION_JSON).content(EMITIR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("FOR-2026-000001"))
                .andExpect(jsonPath("$.items[0].codigoMedicamento").value("MDT-0001"))
                .andExpect(jsonPath("$.items[1].cantidad").value(2));
        mvc.perform(get("/api/v1/formulas/FOR-2026-000001")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/formulas").param("paciente", "PAC-000001").param("estado", "emitida"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(post("/api/v1/formulas/FOR-2026-000001/dispensacion"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DISPENSADA"));
        mvc.perform(patch("/api/v1/formulas/FOR-2026-000001/anulacion")).andExpect(status().isOk());
    }

    @Test
    void medicamentos() throws Exception {
        when(registrar.ejecutar(any())).thenReturn(Datos.acetaminofen());
        when(consultarMed.ejecutar("MDT-0001")).thenReturn(Datos.acetaminofen());
        when(listarMed.ejecutar(null, 0, 20)).thenReturn(new Pagina<>(List.of(Datos.acetaminofen()), 0, 20, 1, 1));
        when(reabastecer.ejecutar("MDT-0001", 50)).thenReturn(Datos.acetaminofen());

        mvc.perform(post("/api/v1/medicamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Acetaminofen\",\"presentacion\":\"Tabletas 500 mg\",\"stock\":500}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(500));
        mvc.perform(get("/api/v1/medicamentos/MDT-0001")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/medicamentos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].codigo").value("MDT-0001"));
        mvc.perform(patch("/api/v1/medicamentos/MDT-0001/stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":50}")).andExpect(status().isOk());
    }

    @Test
    void errores() throws Exception {
        when(consultar.ejecutar("FOR-404")).thenThrow(new NoEncontradoException("FORMULA_NO_ENCONTRADA", "x"));
        when(dispensar.ejecutar("FOR-409")).thenThrow(new ConflictoException("STOCK_INSUFICIENTE", "falta"));
        when(dispensar.ejecutar("FOR-DB")).thenThrow(new DataIntegrityViolationException("ck_stock"));
        when(anular.ejecutar("FOR-500")).thenThrow(new IllegalStateException("x"));
        when(emitir.ejecutar(eq("CIT-CAIDA"), anyList())).thenThrow(new ServicioNoDisponibleException("ms-citas", null));

        mvc.perform(get("/api/v1/formulas/FOR-404")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/formulas/FOR-409/dispensacion")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
        mvc.perform(post("/api/v1/formulas/FOR-DB/dispensacion")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_DATOS"));
        mvc.perform(patch("/api/v1/formulas/FOR-500/anulacion")).andExpect(status().isInternalServerError());
        mvc.perform(post("/api/v1/formulas").contentType(MediaType.APPLICATION_JSON)
                        .content(EMITIR.replace("CIT-2026-000001", "CIT-CAIDA")))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/v1/formulas").contentType(MediaType.APPLICATION_JSON).content("{ mal"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/formulas").param("estado", "perdida")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/formulas").param("size", "x")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/medicamentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"A\",\"presentacion\":\"B\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void cuerposNulos() {
        FormulaController formulas = new FormulaController(emitir, consultar, listar, dispensar, anular, new DtoMapper());
        MedicamentoController meds = new MedicamentoController(registrar, consultarMed, listarMed, reabastecer,
                new DtoMapper());
        when(reabastecer.ejecutar("MDT-0001", null)).thenThrow(new IllegalArgumentException("cantidad"));

        assertThatThrownBy(() -> formulas.emitir(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> meds.reabastecer("MDT-0001", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
