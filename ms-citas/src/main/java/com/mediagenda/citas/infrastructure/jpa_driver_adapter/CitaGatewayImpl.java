package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class CitaGatewayImpl implements CitaGateway {

    private final CitaRepositoryJPA repository;
    private final FranjaRepositoryJPA franjaRepository;
    private final EntidadesMapper mapper;

    @Override
    public Cita guardar(Cita cita) {
        FranjaEntity franja = franjaRepository.getReferenceById(cita.getFranja().getIdFranja());
        return mapper.toDomain(repository.save(mapper.toEntity(cita, franja)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cita> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Cita> listar(FiltroCitas filtro, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return Paginas.desde(repository.findAll(Especificaciones.citas(filtro), solicitud), mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> pendientesDeRecordatorio(LocalDateTime desde, LocalDateTime hasta) {
        return repository.pendientesDeRecordatorio(EstadoCita.AGENDADA,
                        desde.toLocalDate(), desde.toLocalTime(), hasta.toLocalDate(), hasta.toLocalTime())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void actualizarTriage(String codigo, EstadoTriage estado, ResultadoTriage resultado) {
        if (resultado == null) {
            repository.cambiarEstadoTriage(codigo, estado);
        } else {
            repository.completarTriage(codigo, estado, resultado.especialidad(), resultado.prioridad(),
                    resultado.motivo());
        }
    }

    @Override
    public void marcarRecordatorioEnviado(String codigo) {
        repository.marcarRecordatorioEnviado(codigo);
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
