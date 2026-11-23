package com.mediagenda.pacientes.infrastructure.entry_points;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.usecase.ActualizarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ConsultarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.DesactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ListarPacientesUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarPacienteUseCase;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba las 5 APIs y el manejo de errores sin levantar la base de datos:
 * los casos de uso se simulan con Mockito y MockMvc hace las peticiones HTTP.
 */
class PacienteControllerTest {

    private static final String URL = "/api/v1/pacientes";
    private static final String CUERPO_VALIDO = """
            {"tipoDocumento":"CC","numeroDocumento":"1069123456","nombres":"Laura Valentina",
             "apellidos":"Gomez Rojas","correo":"laura.gomez@correo.com","telefono":"3101234567",
             "fechaNacimiento":"1998-05-14","tipoSangre":"O+","alergias":"Penicilina"}
            """;

    private RegistrarPacienteUseCase registrar;
    private ConsultarPacienteUseCase consultar;
    private ListarPacientesUseCase listar;
    private ActualizarPacienteUseCase actualizar;
    private DesactivarPacienteUseCase desactivar;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        registrar = mock(RegistrarPacienteUseCase.class);
        consultar = mock(ConsultarPacienteUseCase.class);
        listar = mock(ListarPacientesUseCase.class);
        actualizar = mock(ActualizarPacienteUseCase.class);
        desactivar = mock(DesactivarPacienteUseCase.class);
        PacienteController controller = new PacienteController(
                registrar, consultar, listar, actualizar, desactivar, new PacienteDtoMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    // ---------- Caminos felices de las 5 APIs ----------

    @Test
    void registrarResponde201() throws Exception {
        when(registrar.ejecutar(any())).thenReturn(PacientesDePrueba.guardado());

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("PAC-000001"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.idPaciente").doesNotExist());
    }

    @Test
    void consultarResponde200() throws Exception {
        when(consultar.ejecutar("PAC-000001")).thenReturn(PacientesDePrueba.guardado());

        mvc.perform(get(URL + "/PAC-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Laura Valentina"))
                .andExpect(jsonPath("$.tipoDocumento").value("CC"));
    }

    @Test
    void listarUsaValoresPorDefecto() throws Exception {
        when(listar.ejecutar(null, 0, 10))
                .thenReturn(new Pagina<>(List.of(PacientesDePrueba.guardado()), 0, 10, 1, 1));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].codigo").value("PAC-000001"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listarConFiltroYPaginacion() throws Exception {
        when(listar.ejecutar("laura", 2, 5)).thenReturn(new Pagina<>(List.<Paciente>of(), 2, 5, 0, 0));

        mvc.perform(get(URL).param("nombre", "laura").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void actualizarResponde200() throws Exception {
        when(actualizar.ejecutar(eq("PAC-000001"), any())).thenReturn(PacientesDePrueba.guardado());

        mvc.perform(put(URL + "/PAC-000001").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PAC-000001"));
    }

    @Test
    void desactivarResponde204() throws Exception {
        mvc.perform(delete(URL + "/PAC-000001"))
                .andExpect(status().isNoContent());

        verify(desactivar).ejecutar("PAC-000001");
    }

    // ---------- Errores ----------

    @Test
    void pacienteInexistenteResponde404() throws Exception {
        when(consultar.ejecutar("PAC-000404")).thenThrow(new PacienteNoEncontradoException("PAC-000404"));

        mvc.perform(get(URL + "/PAC-000404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_NO_ENCONTRADO"));
    }

    @Test
    void duplicadoResponde409() throws Exception {
        when(registrar.ejecutar(any())).thenThrow(new PacienteDuplicadoException("Ya existe"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_DUPLICADO"));
    }

    @Test
    void registroSimultaneoDetectadoPorLaBaseResponde409() throws Exception {
        when(registrar.ejecutar(any())).thenThrow(new DataIntegrityViolationException("uq_paciente_documento"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_DUPLICADO"));
    }

    @Test
    void inactivoResponde409() throws Exception {
        when(actualizar.ejecutar(eq("PAC-000001"), any())).thenThrow(new PacienteInactivoException("PAC-000001"));

        mvc.perform(put(URL + "/PAC-000001").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_INACTIVO"));
    }

    @Test
    void tipoDocumentoInvalidoResponde400() throws Exception {
        String cuerpo = CUERPO_VALIDO.replace("\"CC\"", "\"NIT\"");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    void parametroDePaginaNoNumericoResponde400() throws Exception {
        mvc.perform(get(URL).param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    void errorInesperadoResponde500SinDetallesInternos() throws Exception {
        doThrow(new IllegalStateException("detalle interno")).when(desactivar).ejecutar("PAC-000001");

        mvc.perform(delete(URL + "/PAC-000001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrio un error inesperado"));
    }
}
