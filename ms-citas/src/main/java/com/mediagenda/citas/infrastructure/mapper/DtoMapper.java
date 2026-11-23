package com.mediagenda.citas.infrastructure.mapper;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.SolicitudFranjas;
import com.mediagenda.citas.infrastructure.entry_points.dto.AbrirAgendaPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.CitaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.FranjaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.MedicoPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.MedicoRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.TriageRespuesta;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Function;

/** Convierte entre los JSON del API y el modelo del dominio. */
@Component
public class DtoMapper {

    public Medico toDomain(MedicoPeticion p) {
        if (p == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        return Medico.builder()
                .nombre(p.nombre())
                .especialidad(Especialidad.desde(p.especialidad()))
                .correo(p.correo())
                .build();
    }

    public SolicitudFranjas toDomain(AbrirAgendaPeticion p) {
        if (p == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        int duracion = p.duracionMinutos() == null ? 30 : p.duracionMinutos();
        return new SolicitudFranjas(p.fecha(), p.horaInicio(), p.horaFin(), duracion);
    }

    /** Filtros opcionales en texto: vacio = sin filtro. */
    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static Especialidad especialidadOpcional(String valor) {
        return textoOpcional(valor) == null ? null : Especialidad.desde(valor);
    }

    public static EstadoCita estadoOpcional(String valor) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            return null;
        }
        return Arrays.stream(EstadoCita.values())
                .filter(e -> e.name().equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado invalido: use AGENDADA, CANCELADA o ATENDIDA"));
    }

    public MedicoRespuesta toRespuesta(Medico m) {
        return new MedicoRespuesta(m.getCodigo(), m.getNombre(), m.getEspecialidad().name(), m.getCorreo(),
                m.isActivo());
    }

    public FranjaRespuesta toRespuesta(Franja f) {
        return new FranjaRespuesta(f.getIdFranja(), f.getMedico().getCodigo(), f.getMedico().getNombre(),
                f.getMedico().getEspecialidad().name(), f.getFecha(), f.getHoraInicio(), f.getHoraFin(),
                f.getEstado().name());
    }

    public CitaRespuesta toRespuesta(Cita c) {
        TriageRespuesta triage = new TriageRespuesta(
                c.getEstadoTriage().name(),
                c.getEspecialidadSugerida() == null ? null : c.getEspecialidadSugerida().name(),
                c.getPrioridad() == null ? null : c.getPrioridad().name(),
                c.getMotivoTriage());
        return new CitaRespuesta(c.getCodigo(), c.getCodigoPaciente(), c.getNombrePaciente(), c.getCorreoPaciente(),
                c.getTelefonoPaciente(),
                c.getEstado().name(),
                toRespuesta(c.getFranja()), c.getSintomas(), triage, c.getFechaCreacion());
    }

    public <D, R> PaginaRespuesta<R> toRespuesta(Pagina<D> pagina, Function<D, R> conversion) {
        return new PaginaRespuesta<>(pagina.contenido().stream().map(conversion).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
