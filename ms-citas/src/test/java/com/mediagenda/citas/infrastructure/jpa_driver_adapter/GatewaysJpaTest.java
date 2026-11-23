package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.infrastructure.mapper.EntidadesMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gateways JPA con los repositorios simulados: se prueba la conversion y las llamadas correctas. */
class GatewaysJpaTest {

    private final EntidadesMapper mapper = new EntidadesMapper();
    private MedicoRepositoryJPA medicoRepo;
    private FranjaRepositoryJPA franjaRepo;
    private CitaRepositoryJPA citaRepo;
    private MedicoGatewayImpl medicos;
    private FranjaGatewayImpl franjas;
    private CitaGatewayImpl citas;

    @BeforeEach
    void preparar() {
        medicoRepo = mock(MedicoRepositoryJPA.class);
        franjaRepo = mock(FranjaRepositoryJPA.class);
        citaRepo = mock(CitaRepositoryJPA.class);
        medicos = new MedicoGatewayImpl(medicoRepo, mapper);
        franjas = new FranjaGatewayImpl(franjaRepo, medicoRepo, mapper);
        citas = new CitaGatewayImpl(citaRepo, franjaRepo, mapper);
    }

    private MedicoEntity medicoEntity() {
        return mapper.toEntity(Datos.medico());
    }

    private FranjaEntity franjaEntity() {
        return mapper.toEntity(Datos.franjaManana(), medicoEntity());
    }

    private CitaEntity citaEntity() {
        return mapper.toEntity(Datos.cita(), franjaEntity());
    }

    @Test
    void mapperIdaYVuelta() {
        Cita original = Datos.cita();
        original.setEspecialidadSugerida(Especialidad.NEUROLOGIA);
        original.setPrioridad(Prioridad.MEDIA);
        original.setMotivoTriage("cefalea");

        Cita copia = mapper.toDomain(mapper.toEntity(original, mapper.toEntity(original.getFranja(),
                mapper.toEntity(original.getFranja().getMedico()))));

        assertThat(copia).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void medicos() {
        when(medicoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(medicoRepo.findByCodigo("MED-0001")).thenReturn(Optional.of(medicoEntity()));
        when(medicoRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(medicoEntity()), PageRequest.of(0, 10), 1));
        when(medicoRepo.findByEspecialidad(eq(Especialidad.CARDIOLOGIA), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(medicoRepo.existsByCorreoIgnoreCase("a@b.co")).thenReturn(true);
        when(medicoRepo.siguienteConsecutivo()).thenReturn(4L);

        assertThat(medicos.guardar(Datos.medico()).getCodigo()).isEqualTo("MED-0001");
        assertThat(medicos.buscarPorCodigo("MED-0001")).isPresent();
        Pagina<Medico> todos = medicos.listar(null, 0, 10);
        assertThat(todos.contenido()).hasSize(1);
        assertThat(todos.totalElementos()).isEqualTo(1);
        assertThat(medicos.listar(Especialidad.CARDIOLOGIA, 0, 10).contenido()).isEmpty();
        assertThat(medicos.existeCorreo("a@b.co")).isTrue();
        assertThat(medicos.siguienteConsecutivo()).isEqualTo(4L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void franjas() {
        when(medicoRepo.getReferenceById(any())).thenReturn(medicoEntity());
        when(franjaRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(franjaRepo.findById(10L)).thenReturn(Optional.of(franjaEntity()));
        when(franjaRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(franjaEntity()), PageRequest.of(0, 20), 1));
        when(franjaRepo.haySolapamiento(any(), any(), any(), any())).thenReturn(true);
        when(franjaRepo.cambiarEstado(10L, EstadoFranja.DISPONIBLE, EstadoFranja.RESERVADA)).thenReturn(1);
        when(franjaRepo.cambiarEstado(11L, EstadoFranja.DISPONIBLE, EstadoFranja.RESERVADA)).thenReturn(0);

        List<Franja> guardadas = franjas.guardarTodas(List.of(Datos.franjaManana()));
        assertThat(guardadas).hasSize(1);
        assertThat(guardadas.get(0).getMedico().getCodigo()).isEqualTo("MED-0001");
        assertThat(franjas.buscarPorId(10L)).isPresent();
        assertThat(franjas.listar(new FiltroFranjas(null, null, true), 0, 20).contenido()).hasSize(1);
        assertThat(franjas.existeSolapamiento(Datos.medico(), Datos.MANANA, LocalTime.NOON, LocalTime.MIDNIGHT)).isTrue();
        assertThat(franjas.reservar(10L)).isTrue();
        assertThat(franjas.reservar(11L)).isFalse();
        franjas.liberar(10L);
        verify(franjaRepo).cambiarEstado(10L, EstadoFranja.RESERVADA, EstadoFranja.DISPONIBLE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void citas() {
        when(franjaRepo.getReferenceById(10L)).thenReturn(franjaEntity());
        when(citaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(citaRepo.findByCodigo("CIT-2026-000001")).thenReturn(Optional.of(citaEntity()));
        when(citaRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(citaEntity()), PageRequest.of(0, 10), 1));
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 10, 0);
        when(citaRepo.pendientesDeRecordatorio(EstadoCita.AGENDADA, LocalDate.of(2026, 11, 20), LocalTime.of(10, 0),
                LocalDate.of(2026, 11, 21), LocalTime.of(10, 0))).thenReturn(List.of(citaEntity()));
        when(citaRepo.siguienteConsecutivo()).thenReturn(9L);

        assertThat(citas.guardar(Datos.cita()).getCodigo()).isEqualTo("CIT-2026-000001");
        assertThat(citas.buscarPorCodigo("CIT-2026-000001")).isPresent();
        assertThat(citas.listar(new FiltroCitas(null, null, null), 0, 10).contenido()).hasSize(1);
        assertThat(citas.pendientesDeRecordatorio(desde, desde.plusHours(24))).hasSize(1);
        assertThat(citas.siguienteConsecutivo()).isEqualTo(9L);

        ResultadoTriage resultado = new ResultadoTriage(Especialidad.NEUROLOGIA, Prioridad.ALTA, "motivo");
        citas.actualizarTriage("CIT-2026-000001", EstadoTriage.COMPLETADO, resultado);
        citas.actualizarTriage("CIT-2026-000001", EstadoTriage.FALLIDO, null);
        citas.marcarRecordatorioEnviado("CIT-2026-000001");
        verify(citaRepo).completarTriage("CIT-2026-000001", EstadoTriage.COMPLETADO, Especialidad.NEUROLOGIA,
                Prioridad.ALTA, "motivo");
        verify(citaRepo).cambiarEstadoTriage("CIT-2026-000001", EstadoTriage.FALLIDO);
        verify(citaRepo).marcarRecordatorioEnviado("CIT-2026-000001");
    }
}
