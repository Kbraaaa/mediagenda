package com.mediagenda.citas.infrastructure.mapper;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.infrastructure.jpa_driver_adapter.CitaEntity;
import com.mediagenda.citas.infrastructure.jpa_driver_adapter.FranjaEntity;
import com.mediagenda.citas.infrastructure.jpa_driver_adapter.MedicoEntity;
import org.springframework.stereotype.Component;

/** Convierte entre el modelo del dominio y las entidades JPA. */
@Component
public class EntidadesMapper {

    public Medico toDomain(MedicoEntity e) {
        return Medico.builder()
                .idMedico(e.getIdMedico())
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .especialidad(e.getEspecialidad())
                .correo(e.getCorreo())
                .activo(e.isActivo())
                .build();
    }

    public MedicoEntity toEntity(Medico m) {
        MedicoEntity e = new MedicoEntity();
        e.setIdMedico(m.getIdMedico());
        e.setCodigo(m.getCodigo());
        e.setNombre(m.getNombre());
        e.setEspecialidad(m.getEspecialidad());
        e.setCorreo(m.getCorreo());
        e.setActivo(m.isActivo());
        return e;
    }

    public Franja toDomain(FranjaEntity e) {
        return Franja.builder()
                .idFranja(e.getIdFranja())
                .medico(toDomain(e.getMedico()))
                .fecha(e.getFecha())
                .horaInicio(e.getHoraInicio())
                .horaFin(e.getHoraFin())
                .estado(e.getEstado())
                .build();
    }

    /** medico es la referencia JPA ya cargada (o un proxy) del medico de la franja. */
    public FranjaEntity toEntity(Franja f, MedicoEntity medico) {
        FranjaEntity e = new FranjaEntity();
        e.setIdFranja(f.getIdFranja());
        e.setMedico(medico);
        e.setFecha(f.getFecha());
        e.setHoraInicio(f.getHoraInicio());
        e.setHoraFin(f.getHoraFin());
        e.setEstado(f.getEstado());
        return e;
    }

    public Cita toDomain(CitaEntity e) {
        return Cita.builder()
                .idCita(e.getIdCita())
                .codigo(e.getCodigo())
                .codigoPaciente(e.getCodigoPaciente())
                .nombrePaciente(e.getNombrePaciente())
                .correoPaciente(e.getCorreoPaciente())
                .telefonoPaciente(e.getTelefonoPaciente())
                .franja(toDomain(e.getFranja()))
                .sintomas(e.getSintomas())
                .especialidadSugerida(e.getEspecialidadSugerida())
                .prioridad(e.getPrioridad())
                .motivoTriage(e.getMotivoTriage())
                .estadoTriage(e.getEstadoTriage())
                .estado(e.getEstado())
                .recordatorioEnviado(e.isRecordatorioEnviado())
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }

    /** franja es la referencia JPA de la franja de la cita. */
    public CitaEntity toEntity(Cita c, FranjaEntity franja) {
        CitaEntity e = new CitaEntity();
        e.setIdCita(c.getIdCita());
        e.setCodigo(c.getCodigo());
        e.setCodigoPaciente(c.getCodigoPaciente());
        e.setNombrePaciente(c.getNombrePaciente());
        e.setCorreoPaciente(c.getCorreoPaciente());
        e.setTelefonoPaciente(c.getTelefonoPaciente());
        e.setFranja(franja);
        e.setSintomas(c.getSintomas());
        e.setEspecialidadSugerida(c.getEspecialidadSugerida());
        e.setPrioridad(c.getPrioridad());
        e.setMotivoTriage(c.getMotivoTriage());
        e.setEstadoTriage(c.getEstadoTriage());
        e.setEstado(c.getEstado());
        e.setRecordatorioEnviado(c.isRecordatorioEnviado());
        e.setFechaCreacion(c.getFechaCreacion());
        return e;
    }
}
