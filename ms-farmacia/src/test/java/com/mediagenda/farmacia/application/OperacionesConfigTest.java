package com.mediagenda.farmacia.application;

import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OperacionesConfigTest {

    @Test
    void creaLasOperacionesDeInventario() {
        UseCasesConfig config = new UseCasesConfig();
        assertThat(config.darDeBajaMedicamentoUseCase(mock(MedicamentoGateway.class), mock(TransaccionGateway.class)))
                .isNotNull();
        assertThat(config.verificarDisponibilidadUseCase(mock(FormulaGateway.class))).isNotNull();
    }
}
