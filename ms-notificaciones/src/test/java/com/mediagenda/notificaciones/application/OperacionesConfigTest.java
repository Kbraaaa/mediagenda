package com.mediagenda.notificaciones.application;

import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import com.mediagenda.notificaciones.domain.usecase.Despachador;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OperacionesConfigTest {

    @Test
    void creaLasOperacionesDeSoporte() {
        UseCasesConfig config = new UseCasesConfig();
        NotificacionGateway n = mock(NotificacionGateway.class);
        SolicitudEnvioGateway s = mock(SolicitudEnvioGateway.class);

        assertThat(config.reenviarNotificacionUseCase(n, s, mock(Despachador.class))).isNotNull();
        assertThat(config.corregirDestinatarioUseCase(n, s)).isNotNull();
    }
}
