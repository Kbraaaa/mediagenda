package com.mediagenda.farmacia.application;

import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import com.mediagenda.farmacia.domain.usecase.AnularFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ConsultarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ConsultarMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.DarDeBajaMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.DispensarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.EmitirFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarFormulasUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarMedicamentosUseCase;
import com.mediagenda.farmacia.domain.usecase.ReabastecerMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.RegistrarMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.VerificarDisponibilidadUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso y les entrega sus gateways. */
@Configuration
public class UseCasesConfig {

    @Bean
    public RegistrarMedicamentoUseCase registrarMedicamentoUseCase(MedicamentoGateway m) {
        return new RegistrarMedicamentoUseCase(m);
    }

    @Bean
    public ConsultarMedicamentoUseCase consultarMedicamentoUseCase(MedicamentoGateway m) {
        return new ConsultarMedicamentoUseCase(m);
    }

    @Bean
    public ListarMedicamentosUseCase listarMedicamentosUseCase(MedicamentoGateway m) {
        return new ListarMedicamentosUseCase(m);
    }

    @Bean
    public ReabastecerMedicamentoUseCase reabastecerMedicamentoUseCase(MedicamentoGateway m) {
        return new ReabastecerMedicamentoUseCase(m);
    }

    @Bean
    public EmitirFormulaUseCase emitirFormulaUseCase(CitaGateway c, MedicamentoGateway m, FormulaGateway f,
                                                     EventoGateway e, Clock reloj) {
        return new EmitirFormulaUseCase(c, m, f, e, reloj);
    }

    @Bean
    public ConsultarFormulaUseCase consultarFormulaUseCase(FormulaGateway f) {
        return new ConsultarFormulaUseCase(f);
    }

    @Bean
    public ListarFormulasUseCase listarFormulasUseCase(FormulaGateway f) {
        return new ListarFormulasUseCase(f);
    }

    @Bean
    public DispensarFormulaUseCase dispensarFormulaUseCase(FormulaGateway f, MedicamentoGateway m,
                                                           TransaccionGateway t, EventoGateway e, Clock reloj) {
        return new DispensarFormulaUseCase(f, m, t, e, reloj);
    }

    @Bean
    public AnularFormulaUseCase anularFormulaUseCase(FormulaGateway f) {
        return new AnularFormulaUseCase(f);
    }

    @Bean
    public DarDeBajaMedicamentoUseCase darDeBajaMedicamentoUseCase(MedicamentoGateway m, TransaccionGateway t) {
        return new DarDeBajaMedicamentoUseCase(m, t);
    }

    @Bean
    public VerificarDisponibilidadUseCase verificarDisponibilidadUseCase(FormulaGateway f) {
        return new VerificarDisponibilidadUseCase(f);
    }
}
