package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;

import java.util.Optional;

public interface MedicoGateway {

    Medico guardar(Medico medico);

    Optional<Medico> buscarPorCodigo(String codigo);

    Pagina<Medico> listar(Especialidad especialidad, int pagina, int tamano);

    boolean existeCorreo(String correo);

    long siguienteConsecutivo();
}
