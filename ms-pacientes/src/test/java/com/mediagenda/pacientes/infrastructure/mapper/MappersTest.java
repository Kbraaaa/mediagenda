package com.mediagenda.pacientes.infrastructure.mapper;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacientePeticion;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacienteRespuesta;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PaginaRespuesta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MappersTest {

    private final PacienteMapper mapper = new PacienteMapper();
    private final PacienteDtoMapper dtoMapper = new PacienteDtoMapper();

    @Test
    void entidadIdaYVueltaConservaTodosLosCampos() {
        Paciente original = PacientesDePrueba.guardado();

        Paciente copia = mapper.toDomain(mapper.toEntity(original));

        assertThat(copia).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void peticionADominio() {
        PacientePeticion peticion = new PacientePeticion("ti", "1234567", "Ana", "Ruiz",
                "ana@correo.com", null, LocalDate.of(2012, 3, 1), "B+", null);

        Paciente paciente = dtoMapper.toDomain(peticion);

        assertThat(paciente.getTipoDocumento()).isEqualTo(TipoDocumento.TI);
        assertThat(paciente.getNombres()).isEqualTo("Ana");
        assertThat(paciente.getTipoSangre()).isEqualTo("B+");
    }

    @Test
    void peticionNulaEsInvalida() {
        assertThatThrownBy(() -> dtoMapper.toDomain(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dominioARespuestaYPagina() {
        Paciente paciente = PacientesDePrueba.guardado();

        PacienteRespuesta respuesta = dtoMapper.toRespuesta(paciente);
        PaginaRespuesta<PacienteRespuesta> pagina =
                dtoMapper.toRespuesta(new Pagina<>(List.of(paciente), 2, 20, 41, 3));

        assertThat(respuesta.codigo()).isEqualTo("PAC-000001");
        assertThat(respuesta.tipoDocumento()).isEqualTo("CC");
        assertThat(respuesta.activo()).isTrue();
        assertThat(pagina.contenido()).containsExactly(respuesta);
        assertThat(pagina.pagina()).isEqualTo(2);
        assertThat(pagina.totalElementos()).isEqualTo(41);
    }
}
