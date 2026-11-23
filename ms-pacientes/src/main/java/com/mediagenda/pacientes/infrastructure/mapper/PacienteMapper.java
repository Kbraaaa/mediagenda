package com.mediagenda.pacientes.infrastructure.mapper;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.infrastructure.jpa_driver_adapter.PacienteEntity;
import org.springframework.stereotype.Component;

/** Convierte entre el modelo del dominio y la entidad de base de datos. */
@Component
public class PacienteMapper {

    public PacienteEntity toEntity(Paciente p) {
        PacienteEntity e = new PacienteEntity();
        e.setIdPaciente(p.getIdPaciente());
        e.setCodigo(p.getCodigo());
        e.setTipoDocumento(p.getTipoDocumento());
        e.setNumeroDocumento(p.getNumeroDocumento());
        e.setNombres(p.getNombres());
        e.setApellidos(p.getApellidos());
        e.setCorreo(p.getCorreo());
        e.setTelefono(p.getTelefono());
        e.setFechaNacimiento(p.getFechaNacimiento());
        e.setTipoSangre(p.getTipoSangre());
        e.setAlergias(p.getAlergias());
        e.setActivo(p.isActivo());
        e.setFechaRegistro(p.getFechaRegistro());
        return e;
    }

    public Paciente toDomain(PacienteEntity e) {
        return Paciente.builder()
                .idPaciente(e.getIdPaciente())
                .codigo(e.getCodigo())
                .tipoDocumento(e.getTipoDocumento())
                .numeroDocumento(e.getNumeroDocumento())
                .nombres(e.getNombres())
                .apellidos(e.getApellidos())
                .correo(e.getCorreo())
                .telefono(e.getTelefono())
                .fechaNacimiento(e.getFechaNacimiento())
                .tipoSangre(e.getTipoSangre())
                .alergias(e.getAlergias())
                .activo(e.isActivo())
                .fechaRegistro(e.getFechaRegistro())
                .build();
    }
}
