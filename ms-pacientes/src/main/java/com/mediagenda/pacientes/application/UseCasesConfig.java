package com.mediagenda.pacientes.application;

import com.mediagenda.pacientes.domain.usecase.CambiarDocumentoUseCase;
import com.mediagenda.pacientes.domain.usecase.CompatibilidadSanguineaUseCase;
import com.mediagenda.pacientes.domain.usecase.ReactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarAlergiaUseCase;
import com.mediagenda.pacientes.domain.usecase.VerificarAlergiaUseCase;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import com.mediagenda.pacientes.domain.usecase.ActualizarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ConsultarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.DesactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ListarPacientesUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarPacienteUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Crea los casos de uso y les entrega su gateway. Los casos de uso no tienen anotaciones
 * de Spring: asi el dominio no depende del framework.
 */
@Configuration
public class UseCasesConfig {

    @Bean
    public RegistrarPacienteUseCase registrarPacienteUseCase(PacienteGateway gateway) {
        return new RegistrarPacienteUseCase(gateway);
    }

    @Bean
    public ConsultarPacienteUseCase consultarPacienteUseCase(PacienteGateway gateway) {
        return new ConsultarPacienteUseCase(gateway);
    }

    @Bean
    public ListarPacientesUseCase listarPacientesUseCase(PacienteGateway gateway) {
        return new ListarPacientesUseCase(gateway);
    }

    @Bean
    public ActualizarPacienteUseCase actualizarPacienteUseCase(PacienteGateway gateway) {
        return new ActualizarPacienteUseCase(gateway);
    }

    @Bean
    public DesactivarPacienteUseCase desactivarPacienteUseCase(PacienteGateway gateway) {
        return new DesactivarPacienteUseCase(gateway);
    }

    // ---------- Operaciones de negocio (distintas del CRUD) ----------

    @Bean
    public ReactivarPacienteUseCase reactivarPacienteUseCase(PacienteGateway gateway) {
        return new ReactivarPacienteUseCase(gateway);
    }

    @Bean
    public RegistrarAlergiaUseCase registrarAlergiaUseCase(PacienteGateway gateway) {
        return new RegistrarAlergiaUseCase(gateway);
    }

    @Bean
    public VerificarAlergiaUseCase verificarAlergiaUseCase(PacienteGateway gateway) {
        return new VerificarAlergiaUseCase(gateway);
    }

    @Bean
    public CambiarDocumentoUseCase cambiarDocumentoUseCase(PacienteGateway gateway) {
        // La edad se calcula con la fecha de Colombia, sin importar la zona horaria del servidor
        return new CambiarDocumentoUseCase(gateway, Clock.system(ZoneId.of("America/Bogota")));
    }

    @Bean
    public CompatibilidadSanguineaUseCase compatibilidadSanguineaUseCase(PacienteGateway gateway) {
        return new CompatibilidadSanguineaUseCase(gateway);
    }
}
