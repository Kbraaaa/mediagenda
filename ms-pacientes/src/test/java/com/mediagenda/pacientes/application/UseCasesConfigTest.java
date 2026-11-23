package com.mediagenda.pacientes.application;

import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UseCasesConfigTest {

    @Test
    void creaLosCincoCasosDeUso() {
        UseCasesConfig config = new UseCasesConfig();
        PacienteGateway gateway = mock(PacienteGateway.class);

        assertThat(config.registrarPacienteUseCase(gateway)).isNotNull();
        assertThat(config.consultarPacienteUseCase(gateway)).isNotNull();
        assertThat(config.listarPacientesUseCase(gateway)).isNotNull();
        assertThat(config.actualizarPacienteUseCase(gateway)).isNotNull();
        assertThat(config.desactivarPacienteUseCase(gateway)).isNotNull();
    }
}
