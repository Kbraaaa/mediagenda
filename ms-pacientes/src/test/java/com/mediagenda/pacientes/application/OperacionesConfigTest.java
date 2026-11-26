package com.mediagenda.pacientes.application;

import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OperacionesConfigTest {

    @Test
    void creaLasCincoOperacionesDeNegocio() {
        UseCasesConfig config = new UseCasesConfig();
        PacienteGateway gateway = mock(PacienteGateway.class);

        assertThat(config.reactivarPacienteUseCase(gateway)).isNotNull();
        assertThat(config.registrarAlergiaUseCase(gateway)).isNotNull();
        assertThat(config.verificarAlergiaUseCase(gateway)).isNotNull();
        assertThat(config.cambiarDocumentoUseCase(gateway)).isNotNull();
        assertThat(config.compatibilidadSanguineaUseCase(gateway)).isNotNull();
    }
}
