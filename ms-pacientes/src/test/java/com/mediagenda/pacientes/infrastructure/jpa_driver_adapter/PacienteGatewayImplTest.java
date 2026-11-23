package com.mediagenda.pacientes.infrastructure.jpa_driver_adapter;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteGatewayImplTest {

    private PacienteRepositoryJPA repository;
    private PacienteMapper mapper;
    private PacienteGatewayImpl gateway;

    @BeforeEach
    void preparar() {
        repository = mock(PacienteRepositoryJPA.class);
        mapper = new PacienteMapper();
        gateway = new PacienteGatewayImpl(repository, mapper);
    }

    @Test
    void guardaYDevuelveLoQueQuedoEnLaBase() {
        Paciente paciente = PacientesDePrueba.guardado();
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Paciente guardado = gateway.guardar(paciente);

        assertThat(guardado.getCodigo()).isEqualTo("PAC-000001");
        assertThat(guardado.getIdPaciente()).isEqualTo(paciente.getIdPaciente());
    }

    @Test
    void buscaPorCodigo() {
        when(repository.findByCodigo("PAC-000001"))
                .thenReturn(Optional.of(mapper.toEntity(PacientesDePrueba.guardado())));
        when(repository.findByCodigo("PAC-000002")).thenReturn(Optional.empty());

        assertThat(gateway.buscarPorCodigo("PAC-000001")).isPresent();
        assertThat(gateway.buscarPorCodigo("PAC-000002")).isEmpty();
    }

    @Test
    void listaSinFiltroOrdenadoPorCodigo() {
        PacienteEntity entidad = mapper.toEntity(PacientesDePrueba.guardado());
        when(repository.findByActivoTrue(any()))
                .thenReturn(new PageImpl<>(List.of(entidad), PageRequest.of(0, 10), 1));

        Pagina<Paciente> pagina = gateway.listarActivos(null, 0, 10);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
        ArgumentCaptor<Pageable> solicitud = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByActivoTrue(solicitud.capture());
        assertThat(solicitud.getValue().getSort().getOrderFor("codigo")).isNotNull();
    }

    @Test
    void listaConFiltroDeNombreEnMinusculas() {
        when(repository.buscarActivosPorNombre(eq("%laura%"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        Pagina<Paciente> pagina = gateway.listarActivos("LAURA", 1, 5);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(5);
    }

    @Test
    void consultasDeExistencia() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, "123")).thenReturn(true);
        when(repository.existsByCorreoIgnoreCase("a@b.co")).thenReturn(true);
        when(repository.existsByCorreoIgnoreCaseAndCodigoNot("a@b.co", "PAC-000001")).thenReturn(false);
        when(repository.siguienteConsecutivo()).thenReturn(7L);

        assertThat(gateway.existeDocumento(TipoDocumento.CC, "123")).isTrue();
        assertThat(gateway.existeCorreo("a@b.co")).isTrue();
        assertThat(gateway.existeCorreoEnOtroPaciente("a@b.co", "PAC-000001")).isFalse();
        assertThat(gateway.siguienteConsecutivo()).isEqualTo(7L);
    }
}
