package com.mediagenda.citas.application;

import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.mediagenda.citas.domain.model.gateway.SolicitudTriageGateway;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;
import com.mediagenda.citas.domain.usecase.AbrirAgendaUseCase;
import com.mediagenda.citas.domain.usecase.AgendarCitaUseCase;
import com.mediagenda.citas.domain.usecase.AtenderCitaUseCase;
import com.mediagenda.citas.domain.usecase.CancelarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarMedicoUseCase;
import com.mediagenda.citas.domain.usecase.EnviarRecordatoriosUseCase;
import com.mediagenda.citas.domain.usecase.ListarCitasUseCase;
import com.mediagenda.citas.domain.usecase.ListarFranjasUseCase;
import com.mediagenda.citas.domain.usecase.ListarMedicosUseCase;
import com.mediagenda.citas.domain.usecase.ProcesarTriageUseCase;
import com.mediagenda.citas.domain.usecase.RegistrarMedicoUseCase;
import com.mediagenda.citas.domain.usecase.ReglasAgenda;
import com.mediagenda.citas.domain.usecase.ReprogramarCitaUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso y les entrega sus gateways. El dominio no tiene anotaciones de Spring. */
@Configuration
public class UseCasesConfig {

    @Bean
    public ReglasAgenda reglasAgenda(Clock reloj, FestivoGateway festivoGateway) {
        return new ReglasAgenda(reloj, festivoGateway);
    }

    @Bean
    public RegistrarMedicoUseCase registrarMedicoUseCase(MedicoGateway medicos) {
        return new RegistrarMedicoUseCase(medicos);
    }

    @Bean
    public ConsultarMedicoUseCase consultarMedicoUseCase(MedicoGateway medicos) {
        return new ConsultarMedicoUseCase(medicos);
    }

    @Bean
    public ListarMedicosUseCase listarMedicosUseCase(MedicoGateway medicos) {
        return new ListarMedicosUseCase(medicos);
    }

    @Bean
    public AbrirAgendaUseCase abrirAgendaUseCase(MedicoGateway medicos, FranjaGateway franjas, ReglasAgenda reglas) {
        return new AbrirAgendaUseCase(medicos, franjas, reglas);
    }

    @Bean
    public ListarFranjasUseCase listarFranjasUseCase(FranjaGateway franjas) {
        return new ListarFranjasUseCase(franjas);
    }

    @Bean
    public AgendarCitaUseCase agendarCitaUseCase(PacienteGateway pacientes, FranjaGateway franjas,
                                                 CitaGateway citas, EventoGateway eventos,
                                                 SolicitudTriageGateway triage, ReglasAgenda reglas) {
        return new AgendarCitaUseCase(pacientes, franjas, citas, eventos, triage, reglas);
    }

    @Bean
    public ConsultarCitaUseCase consultarCitaUseCase(CitaGateway citas) {
        return new ConsultarCitaUseCase(citas);
    }

    @Bean
    public ListarCitasUseCase listarCitasUseCase(CitaGateway citas) {
        return new ListarCitasUseCase(citas);
    }

    @Bean
    public ReprogramarCitaUseCase reprogramarCitaUseCase(CitaGateway citas, FranjaGateway franjas,
                                                         EventoGateway eventos, ReglasAgenda reglas) {
        return new ReprogramarCitaUseCase(citas, franjas, eventos, reglas);
    }

    @Bean
    public CancelarCitaUseCase cancelarCitaUseCase(CitaGateway citas, FranjaGateway franjas, EventoGateway eventos) {
        return new CancelarCitaUseCase(citas, franjas, eventos);
    }

    @Bean
    public AtenderCitaUseCase atenderCitaUseCase(CitaGateway citas) {
        return new AtenderCitaUseCase(citas);
    }

    @Bean
    public ProcesarTriageUseCase procesarTriageUseCase(CitaGateway citas, TriageGateway triage, EventoGateway eventos,
                                                       @Value("${triage.intentos:3}") int intentos) {
        return new ProcesarTriageUseCase(citas, triage, eventos, intentos);
    }

    @Bean
    public EnviarRecordatoriosUseCase enviarRecordatoriosUseCase(CitaGateway citas, EventoGateway eventos,
                                                                 ReglasAgenda reglas) {
        return new EnviarRecordatoriosUseCase(citas, eventos, reglas);
    }
}
