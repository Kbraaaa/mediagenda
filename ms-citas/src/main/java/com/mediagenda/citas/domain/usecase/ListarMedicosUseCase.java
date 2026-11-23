package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import lombok.RequiredArgsConstructor;

/** Lista medicos paginados; la especialidad es un filtro opcional (null = todas). */
@RequiredArgsConstructor
public class ListarMedicosUseCase {

    private final MedicoGateway medicoGateway;

    public Pagina<Medico> ejecutar(Especialidad especialidad, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        return medicoGateway.listar(especialidad, pagina, tamano);
    }
}
