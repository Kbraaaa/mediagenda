package com.mediagenda.citas.application;

import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import com.mediagenda.citas.domain.usecase.ProcesarTriageUseCase;
import com.mediagenda.citas.domain.usecase.ReglasAgenda;
import com.mediagenda.citas.infrastructure.ia.GeminiTriageAdapter;
import com.mediagenda.citas.infrastructure.ia.TriagePorReglasAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigTest {

    @Test
    void creaTodosLosCasosDeUso() {
        UseCasesConfig c = new UseCasesConfig();
        MedicoGateway medicos = mock(MedicoGateway.class);
        FranjaGateway franjas = mock(FranjaGateway.class);
        CitaGateway citas = mock(CitaGateway.class);
        EventoGateway eventos = mock(EventoGateway.class);
        ReglasAgenda reglas = c.reglasAgenda(java.time.Clock.systemUTC(), mock(FestivoGateway.class));

        assertThat(reglas).isNotNull();
        assertThat(c.registrarMedicoUseCase(medicos)).isNotNull();
        assertThat(c.consultarMedicoUseCase(medicos)).isNotNull();
        assertThat(c.listarMedicosUseCase(medicos)).isNotNull();
        assertThat(c.abrirAgendaUseCase(medicos, franjas, reglas)).isNotNull();
        assertThat(c.listarFranjasUseCase(franjas)).isNotNull();
        assertThat(c.agendarCitaUseCase(mock(PacienteGateway.class), franjas, citas, eventos,
                mock(SolicitudTriageGateway.class), reglas)).isNotNull();
        assertThat(c.consultarCitaUseCase(citas)).isNotNull();
        assertThat(c.listarCitasUseCase(citas)).isNotNull();
        assertThat(c.reprogramarCitaUseCase(citas, franjas, eventos, reglas)).isNotNull();
        assertThat(c.cancelarCitaUseCase(citas, franjas, eventos)).isNotNull();
        assertThat(c.atenderCitaUseCase(citas)).isNotNull();
        assertThat(c.procesarTriageUseCase(citas, mock(TriageGateway.class), eventos, 3)).isNotNull();
        assertThat(c.enviarRecordatoriosUseCase(citas, eventos, reglas)).isNotNull();
    }

    @Test
    void creaLosAdaptadores() {
        InfraestructuraConfig c = new InfraestructuraConfig();

        assertThat(c.reloj("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(c.pacienteGateway("http://localhost:8081")).isNotNull();
        assertThat(c.festivoGateway("http://localhost:9", "CO", java.time.Clock.systemUTC())).isNotNull();
        assertThat(c.eventosExchange("mediagenda.eventos").getName()).isEqualTo("mediagenda.eventos");
        assertThat(c.eventoGateway(mock(RabbitTemplate.class), "mediagenda.eventos", "rabbit", "", "us-east-1"))
                .isInstanceOf(com.mediagenda.citas.infrastructure.eventos.RabbitEventoAdapter.class);
        assertThat(c.eventoGateway(mock(RabbitTemplate.class), "mediagenda.eventos", "SQS",
                "https://sqs.us-east-1.amazonaws.com/123/cola", "us-east-1"))
                .isInstanceOf(com.mediagenda.citas.infrastructure.eventos.SqsEventoAdapter.class);
        ThreadPoolTaskExecutor executor = c.triageExecutor();
        assertThat(executor.getMaxPoolSize()).isEqualTo(1);
        assertThat(c.solicitudTriageGateway(mock(ProcesarTriageUseCase.class))).isNotNull();
    }

    @Test
    void sinClaveDeGeminiSeUsaElTriagePorReglas() {
        InfraestructuraConfig c = new InfraestructuraConfig();
        String url = "https://generativelanguage.googleapis.com";

        assertThat(c.triageGateway(true, "clave", "gemini-2.5-flash", url, 0)).isInstanceOf(GeminiTriageAdapter.class);
        assertThat(c.triageGateway(true, " ", "gemini-2.5-flash", url, 0)).isInstanceOf(TriagePorReglasAdapter.class);
        assertThat(c.triageGateway(true, null, "gemini-2.5-flash", url, 0)).isInstanceOf(TriagePorReglasAdapter.class);
        assertThat(c.triageGateway(false, "clave", "gemini-2.5-flash", url, 0)).isInstanceOf(TriagePorReglasAdapter.class);
    }
}
