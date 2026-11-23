package com.mediagenda.pacientes.infrastructure.mapper;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacientePeticion;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacienteRespuesta;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PaginaRespuesta;
import org.springframework.stereotype.Component;

/** Convierte entre los JSON del API (peticion y respuesta) y el modelo del dominio. */
@Component
public class PacienteDtoMapper {

    public Paciente toDomain(PacientePeticion peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        return Paciente.builder()
                .tipoDocumento(TipoDocumento.desde(peticion.tipoDocumento()))
                .numeroDocumento(peticion.numeroDocumento())
                .nombres(peticion.nombres())
                .apellidos(peticion.apellidos())
                .correo(peticion.correo())
                .telefono(peticion.telefono())
                .fechaNacimiento(peticion.fechaNacimiento())
                .tipoSangre(peticion.tipoSangre())
                .alergias(peticion.alergias())
                .build();
    }

    public PacienteRespuesta toRespuesta(Paciente p) {
        return new PacienteRespuesta(
                p.getCodigo(),
                p.getTipoDocumento().name(),
                p.getNumeroDocumento(),
                p.getNombres(),
                p.getApellidos(),
                p.getCorreo(),
                p.getTelefono(),
                p.getFechaNacimiento(),
                p.getTipoSangre(),
                p.getAlergias(),
                p.isActivo(),
                p.getFechaRegistro());
    }

    public PaginaRespuesta<PacienteRespuesta> toRespuesta(Pagina<Paciente> pagina) {
        return new PaginaRespuesta<>(
                pagina.contenido().stream().map(this::toRespuesta).toList(),
                pagina.pagina(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }
}
