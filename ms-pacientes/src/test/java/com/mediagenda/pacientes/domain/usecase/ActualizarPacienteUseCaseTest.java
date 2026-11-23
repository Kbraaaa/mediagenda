package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizarPacienteUseCaseTest {

    private static final String CODIGO = "PAC-000001";

    private PacienteGateway gateway;
    private ActualizarPacienteUseCase useCase;

    @BeforeEach
    void preparar() {
        gateway = mock(PacienteGateway.class);
        useCase = new ActualizarPacienteUseCase(gateway);
        when(gateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void actualizaDatosYConservaIdentidad() {
        Paciente actual = PacientesDePrueba.guardado();
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(actual));
        Paciente nuevos = PacientesDePrueba.datosValidos().toBuilder()
                .correo("NUEVO@correo.com")
                .tipoSangre("A-")
                .alergias(null)
                .build();

        Paciente resultado = useCase.ejecutar(CODIGO, nuevos);

        assertThat(resultado.getCorreo()).isEqualTo("nuevo@correo.com");
        assertThat(resultado.getTipoSangre()).isEqualTo("A-");
        assertThat(resultado.getAlergias()).isNull();
        assertThat(resultado.getCodigo()).isEqualTo(CODIGO);
        assertThat(resultado.getIdPaciente()).isEqualTo(actual.getIdPaciente());
        assertThat(resultado.getFechaRegistro()).isEqualTo(actual.getFechaRegistro());
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    void pacienteInexistente() {
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, PacientesDePrueba.datosValidos()))
                .isInstanceOf(PacienteNoEncontradoException.class);
    }

    @Test
    void pacienteInactivoNoSeModifica() {
        Paciente inactivo = PacientesDePrueba.guardado().toBuilder().activo(false).build();
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, PacientesDePrueba.datosValidos()))
                .isInstanceOf(PacienteInactivoException.class);
        verify(gateway, never()).guardar(any());
    }

    @Test
    void noPermiteCambiarElNumeroDeDocumento() {
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(PacientesDePrueba.guardado()));
        Paciente otroDocumento = PacientesDePrueba.datosValidos().toBuilder().numeroDocumento("99999999").build();

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, otroDocumento))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
    }

    @Test
    void noPermiteCambiarElTipoDeDocumento() {
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(PacientesDePrueba.guardado()));
        Paciente otroTipo = PacientesDePrueba.datosValidos().toBuilder().tipoDocumento(TipoDocumento.CE).build();

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, otroTipo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCorreoDeOtroPaciente() {
        when(gateway.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(PacientesDePrueba.guardado()));
        when(gateway.existeCorreoEnOtroPaciente("laura.gomez@correo.com", CODIGO)).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(CODIGO, PacientesDePrueba.datosValidos()))
                .isInstanceOf(PacienteDuplicadoException.class);
    }
}
