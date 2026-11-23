package com.mediagenda.citas.infrastructure.mapper;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.SolicitudFranjas;
import com.mediagenda.citas.infrastructure.entry_points.dto.AbrirAgendaPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.MedicoPeticion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoMapperTest {

    private final DtoMapper mapper = new DtoMapper();

    @Test
    void peticionesNulasSonInvalidas() {
        assertThatThrownBy(() -> mapper.toDomain((MedicoPeticion) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toDomain((AbrirAgendaPeticion) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duracionPorDefectoYExplicita() {
        LocalDate fecha = LocalDate.of(2026, 11, 21);
        SolicitudFranjas porDefecto = mapper.toDomain(new AbrirAgendaPeticion(fecha, LocalTime.NOON, LocalTime.MAX, null));
        SolicitudFranjas explicita = mapper.toDomain(new AbrirAgendaPeticion(fecha, LocalTime.NOON, LocalTime.MAX, 20));

        assertThat(porDefecto.duracionMinutos()).isEqualTo(30);
        assertThat(explicita.duracionMinutos()).isEqualTo(20);
    }

    @Test
    void filtrosOpcionales() {
        assertThat(DtoMapper.textoOpcional(null)).isNull();
        assertThat(DtoMapper.textoOpcional("  ")).isNull();
        assertThat(DtoMapper.textoOpcional(" MED-0001 ")).isEqualTo("MED-0001");
        assertThat(DtoMapper.especialidadOpcional("")).isNull();
        assertThat(DtoMapper.especialidadOpcional("pediatria")).isEqualTo(Especialidad.PEDIATRIA);
        assertThat(DtoMapper.estadoOpcional(null)).isNull();
        assertThat(DtoMapper.estadoOpcional("cancelada")).isEqualTo(EstadoCita.CANCELADA);
        assertThatThrownBy(() -> DtoMapper.estadoOpcional("perdida")).isInstanceOf(IllegalArgumentException.class);
    }
}
