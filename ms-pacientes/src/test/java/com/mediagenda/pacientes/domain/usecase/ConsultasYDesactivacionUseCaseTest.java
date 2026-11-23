package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultasYDesactivacionUseCaseTest {

    private PacienteGateway gateway;

    @BeforeEach
    void preparar() {
        gateway = mock(PacienteGateway.class);
    }

    @Test
    void consultaPacienteExistente() {
        when(gateway.buscarPorCodigo("PAC-000001")).thenReturn(Optional.of(PacientesDePrueba.guardado()));

        Paciente paciente = new ConsultarPacienteUseCase(gateway).ejecutar("PAC-000001");

        assertThat(paciente.getNombres()).isEqualTo("Laura Valentina");
    }

    @Test
    void consultaPacienteInexistente() {
        when(gateway.buscarPorCodigo("PAC-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ConsultarPacienteUseCase(gateway).ejecutar("PAC-999999"))
                .isInstanceOf(PacienteNoEncontradoException.class)
                .hasMessageContaining("PAC-999999");
    }

    @Test
    void listaSinFiltroCuandoElNombreEstaVacio() {
        Pagina<Paciente> pagina = new Pagina<>(List.of(PacientesDePrueba.guardado()), 0, 10, 1, 1);
        when(gateway.listarActivos(isNull(), anyInt(), anyInt())).thenReturn(pagina);

        Pagina<Paciente> resultado = new ListarPacientesUseCase(gateway).ejecutar("   ", 0, 10);

        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void listaConFiltroSinEspacios() {
        Pagina<Paciente> vacia = new Pagina<>(List.of(), 0, 5, 0, 0);
        when(gateway.listarActivos("laura", 0, 5)).thenReturn(vacia);

        assertThat(new ListarPacientesUseCase(gateway).ejecutar(" laura ", 0, 5)).isSameAs(vacia);
        assertThat(new ListarPacientesUseCase(gateway).ejecutar(null, 0, 5)).isNull();
    }

    @Test
    void listarRechazaTamanoInvalido() {
        assertThatThrownBy(() -> new ListarPacientesUseCase(gateway).ejecutar(null, 0, 500))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void desactivaPacienteActivo() {
        Paciente activo = PacientesDePrueba.guardado();
        when(gateway.buscarPorCodigo("PAC-000001")).thenReturn(Optional.of(activo));

        new DesactivarPacienteUseCase(gateway).ejecutar("PAC-000001");

        assertThat(activo.isActivo()).isFalse();
        verify(gateway).guardar(activo);
    }

    @Test
    void desactivarDosVecesNoEsError() {
        Paciente inactivo = PacientesDePrueba.guardado().toBuilder().activo(false).build();
        when(gateway.buscarPorCodigo("PAC-000001")).thenReturn(Optional.of(inactivo));

        new DesactivarPacienteUseCase(gateway).ejecutar("PAC-000001");

        verify(gateway, never()).guardar(any());
    }

    @Test
    void desactivarPacienteInexistente() {
        when(gateway.buscarPorCodigo("PAC-000404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DesactivarPacienteUseCase(gateway).ejecutar("PAC-000404"))
                .isInstanceOf(PacienteNoEncontradoException.class);
    }
}
