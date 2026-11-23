package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import lombok.RequiredArgsConstructor;

/** Lista medicamentos activos, paginados, con filtro opcional por nombre. */
@RequiredArgsConstructor
public class ListarMedicamentosUseCase {

    private final MedicamentoGateway medicamentos;

    public Pagina<Medicamento> ejecutar(String nombre, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        String filtro = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return medicamentos.listar(filtro, pagina, tamano);
    }
}
