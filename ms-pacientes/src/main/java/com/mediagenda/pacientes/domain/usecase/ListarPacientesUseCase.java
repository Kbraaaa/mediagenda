package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

/** Lista los pacientes activos, paginado y con filtro opcional por nombre. */
@RequiredArgsConstructor
public class ListarPacientesUseCase {

    private final PacienteGateway pacienteGateway;

    public Pagina<Paciente> ejecutar(String nombre, int pagina, int tamano) {
        Pagina.validarParametros(pagina, tamano);
        String filtro = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return pacienteGateway.listarActivos(filtro, pagina, tamano);
    }
}
