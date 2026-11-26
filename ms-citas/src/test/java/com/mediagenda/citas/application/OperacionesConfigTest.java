package com.mediagenda.citas.application;

import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OperacionesConfigTest {

    @Test
    void creaElCasoDeUsoDeReintentoDeTriage() {
        assertThat(new UseCasesConfig().reintentarTriageUseCase(mock(CitaGateway.class),
                mock(SolicitudTriageGateway.class))).isNotNull();
    }
}
