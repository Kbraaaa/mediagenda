package com.mediagenda.pacientes.infrastructure.entry_points;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.CompatibilidadSanguinea;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.VerificacionAlergia;
import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.exception.ReglaNegocioException;
import com.mediagenda.pacientes.domain.usecase.CambiarDocumentoUseCase;
import com.mediagenda.pacientes.domain.usecase.CompatibilidadSanguineaUseCase;
import com.mediagenda.pacientes.domain.usecase.ReactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarAlergiaUseCase;
import com.mediagenda.pacientes.domain.usecase.VerificarAlergiaUseCase;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las 5 operaciones de negocio por HTTP: rutas, cuerpos, respuestas y codigos de error. */
class OperacionesPacienteControllerTest {

    private static final String URL = "/api/v1/pacientes/PAC-000001";

    private ReactivarPacienteUseCase reactivar;
    private RegistrarAlergiaUseCase registrarAlergia;
    private VerificarAlergiaUseCase verificarAlergia;
    private CambiarDocumentoUseCase cambiarDocumento;
    private CompatibilidadSanguineaUseCase compatibilidad;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        reactivar = mock(ReactivarPacienteUseCase.class);
        registrarAlergia = mock(RegistrarAlergiaUseCase.class);
        verificarAlergia = mock(VerificarAlergiaUseCase.class);
        cambiarDocumento = mock(CambiarDocumentoUseCase.class);
        compatibilidad = mock(CompatibilidadSanguineaUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperacionesPacienteController(
                        reactivar, registrarAlergia, verificarAlergia, cambiarDocumento, compatibilidad,
                        new PacienteDtoMapper()))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void reactivacionResponde200() throws Exception {
        when(reactivar.ejecutar("PAC-000001")).thenReturn(PacientesDePrueba.guardado());

        mvc.perform(patch(URL + "/reactivacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void reactivarPacienteActivoResponde409() throws Exception {
        when(reactivar.ejecutar("PAC-000001"))
                .thenThrow(new ConflictoPacienteException("PACIENTE_YA_ACTIVO", "ya esta activo"));

        mvc.perform(patch(URL + "/reactivacion"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_YA_ACTIVO"));
    }

    @Test
    void registrarAlergiaResponde200ConLaLista() throws Exception {
        when(registrarAlergia.ejecutar("PAC-000001", "Sulfas"))
                .thenReturn(PacientesDePrueba.guardado().toBuilder().alergias("Penicilina, Sulfas").build());

        mvc.perform(post(URL + "/alergias").contentType(MediaType.APPLICATION_JSON).content("{\"alergia\":\"Sulfas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias").value("Penicilina, Sulfas"));
    }

    @Test
    void registrarAlergiaSinCuerpoOConDatoInvalidoResponde400() throws Exception {
        when(registrarAlergia.ejecutar("PAC-000001", null)).thenThrow(new IllegalArgumentException("obligatorio"));

        mvc.perform(post(URL + "/alergias").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL + "/alergias").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificacionDeAlergiaDevuelveAlertaYCoincidencias() throws Exception {
        when(verificarAlergia.ejecutar("PAC-000001", "Amoxicilina")).thenReturn(new VerificacionAlergia(
                "PAC-000001", "Amoxicilina", true, List.of("Penicilina (misma familia: penicilinas)")));

        mvc.perform(get(URL + "/alergias/verificacion").param("sustancia", "Amoxicilina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerta").value(true))
                .andExpect(jsonPath("$.coincidencias[0]").value("Penicilina (misma familia: penicilinas)"));
    }

    @Test
    void verificacionDePacienteInexistenteResponde404() throws Exception {
        when(verificarAlergia.ejecutar("PAC-000001", "Ibuprofeno")).thenThrow(new PacienteNoEncontradoException("PAC-000001"));

        mvc.perform(get(URL + "/alergias/verificacion").param("sustancia", "Ibuprofeno"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambioDeDocumentoResponde200ConLaCedula() throws Exception {
        when(cambiarDocumento.ejecutar("PAC-000001", "1012345678")).thenReturn(PacientesDePrueba.guardado().toBuilder()
                .tipoDocumento(TipoDocumento.CC).numeroDocumento("1012345678").build());

        mvc.perform(post(URL + "/cambio-documento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"1012345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.numeroDocumento").value("1012345678"));
    }

    @Test
    void cambioDeDocumentoDeMenorResponde422() throws Exception {
        when(cambiarDocumento.ejecutar(org.mockito.ArgumentMatchers.eq("PAC-000001"), isNull()))
                .thenThrow(new IllegalArgumentException("numero invalido"));
        when(cambiarDocumento.ejecutar("PAC-000001", "1012345678"))
                .thenThrow(new ReglaNegocioException("MENOR_DE_EDAD", "tiene 16 anos"));

        mvc.perform(post(URL + "/cambio-documento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"1012345678\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MENOR_DE_EDAD"));
        mvc.perform(post(URL + "/cambio-documento").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compatibilidadSanguineaResponde200() throws Exception {
        when(compatibilidad.ejecutar("PAC-000001", "PAC-000002")).thenReturn(
                CompatibilidadSanguinea.evaluar("PAC-000001", "AB+", "PAC-000002", "O-"));

        mvc.perform(get(URL + "/compatibilidad-sanguinea").param("donante", "PAC-000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true))
                .andExpect(jsonPath("$.tipoSangreReceptor").value("AB+"))
                .andExpect(jsonPath("$.tipoSangreDonante").value("O-"));
    }

    @Test
    void compatibilidadSinTipoDeSangreResponde422() throws Exception {
        when(compatibilidad.ejecutar("PAC-000001", "PAC-000002"))
                .thenThrow(new ReglaNegocioException("TIPO_SANGRE_NO_REGISTRADO", "sin tipo"));

        mvc.perform(get(URL + "/compatibilidad-sanguinea").param("donante", "PAC-000002"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TIPO_SANGRE_NO_REGISTRADO"));
    }
}
