package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import com.mediagenda.citas.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class MedicoGatewayImpl implements MedicoGateway {

    private final MedicoRepositoryJPA repository;
    private final EntidadesMapper mapper;

    @Override
    public Medico guardar(Medico medico) {
        return mapper.toDomain(repository.save(mapper.toEntity(medico)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medico> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Medico> listar(Especialidad especialidad, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("codigo"));
        var resultado = especialidad == null
                ? repository.findAll(solicitud)
                : repository.findByEspecialidad(especialidad, solicitud);
        return Paginas.desde(resultado, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        return repository.existsByCorreoIgnoreCase(correo);
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
