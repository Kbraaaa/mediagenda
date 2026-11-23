package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class FranjaGatewayImpl implements FranjaGateway {

    private final FranjaRepositoryJPA repository;
    private final MedicoRepositoryJPA medicoRepository;
    private final EntidadesMapper mapper;

    @Override
    public List<Franja> guardarTodas(List<Franja> franjas) {
        List<FranjaEntity> entidades = franjas.stream()
                .map(f -> mapper.toEntity(f, medicoRepository.getReferenceById(f.getMedico().getIdMedico())))
                .toList();
        return repository.saveAll(entidades).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Franja> buscarPorId(Long idFranja) {
        return repository.findById(idFranja).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Franja> listar(FiltroFranjas filtro, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("fecha", "horaInicio"));
        return Paginas.desde(repository.findAll(Especificaciones.franjas(filtro), solicitud), mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeSolapamiento(Medico medico, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return repository.haySolapamiento(medico.getIdMedico(), fecha, inicio, fin);
    }

    @Override
    public boolean reservar(Long idFranja) {
        return repository.cambiarEstado(idFranja, EstadoFranja.DISPONIBLE, EstadoFranja.RESERVADA) == 1;
    }

    @Override
    public void liberar(Long idFranja) {
        repository.cambiarEstado(idFranja, EstadoFranja.RESERVADA, EstadoFranja.DISPONIBLE);
    }
}
