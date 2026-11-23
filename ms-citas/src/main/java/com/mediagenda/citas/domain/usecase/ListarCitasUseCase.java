package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import lombok.RequiredArgsConstructor;

/** Lista citas paginadas, filtrando opcionalmente por paciente, medico y estado. */
@RequiredArgsConstructor
public class ListarCitasUseCase {

    private final CitaGateway citaGateway;

    public Pagina<Cita> ejecutar(FiltroCitas filtro, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        return citaGateway.listar(filtro, pagina, tamano);
    }
}
