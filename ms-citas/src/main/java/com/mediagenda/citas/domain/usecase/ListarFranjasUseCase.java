package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import lombok.RequiredArgsConstructor;

/** Lista franjas de agenda (por medico, fecha y disponibilidad), paginadas. */
@RequiredArgsConstructor
public class ListarFranjasUseCase {

    private final FranjaGateway franjaGateway;

    public Pagina<Franja> ejecutar(FiltroFranjas filtro, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        return franjaGateway.listar(filtro, pagina, tamano);
    }
}
