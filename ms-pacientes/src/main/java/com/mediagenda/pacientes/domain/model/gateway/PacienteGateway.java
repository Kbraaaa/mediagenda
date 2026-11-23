package com.mediagenda.pacientes.domain.model.gateway;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.TipoDocumento;

import java.util.Optional;

/**
 * Puerto de salida: lo que el dominio necesita de la base de datos, sin saber que es JPA.
 */
public interface PacienteGateway {

    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorCodigo(String codigo);

    Pagina<Paciente> listarActivos(String nombre, int pagina, int tamano);

    boolean existeDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existeCorreo(String correo);

    boolean existeCorreoEnOtroPaciente(String correo, String codigoPaciente);

    long siguienteConsecutivo();
}
