package com.mediagenda.farmacia.application;

import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigTest {

    @Test
    void casosDeUsoYAdaptadores() {
        UseCasesConfig u = new UseCasesConfig();
        MedicamentoGateway m = mock(MedicamentoGateway.class);
        FormulaGateway f = mock(FormulaGateway.class);
        EventoGateway e = mock(EventoGateway.class);
        Clock reloj = Clock.systemUTC();

        assertThat(u.registrarMedicamentoUseCase(m)).isNotNull();
        assertThat(u.consultarMedicamentoUseCase(m)).isNotNull();
        assertThat(u.listarMedicamentosUseCase(m)).isNotNull();
        assertThat(u.reabastecerMedicamentoUseCase(m)).isNotNull();
        assertThat(u.emitirFormulaUseCase(mock(CitaGateway.class), m, f, e, reloj)).isNotNull();
        assertThat(u.consultarFormulaUseCase(f)).isNotNull();
        assertThat(u.listarFormulasUseCase(f)).isNotNull();
        assertThat(u.dispensarFormulaUseCase(f, m, mock(TransaccionGateway.class), e, reloj)).isNotNull();
        assertThat(u.anularFormulaUseCase(f)).isNotNull();

        InfraestructuraConfig i = new InfraestructuraConfig();
        assertThat(i.reloj("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(i.citaGateway("http://localhost:8082")).isNotNull();
        assertThat(i.transaccionGateway(mock(PlatformTransactionManager.class))).isNotNull();
        assertThat(i.eventosExchange("mediagenda.eventos").getName()).isEqualTo("mediagenda.eventos");
        assertThat(i.eventoGateway(mock(RabbitTemplate.class), "mediagenda.eventos", "rabbit", "", "us-east-1"))
                .isInstanceOf(com.mediagenda.farmacia.infrastructure.eventos.RabbitEventoAdapter.class);
        assertThat(i.eventoGateway(mock(RabbitTemplate.class), "mediagenda.eventos", "SQS",
                "https://sqs.us-east-1.amazonaws.com/123/cola", "us-east-1"))
                .isInstanceOf(com.mediagenda.farmacia.infrastructure.eventos.SqsEventoAdapter.class);
    }
}
