package com.mediagenda.pacientes.infrastructure.jpa_driver_adapter;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.Pagina;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** Adaptador: implementa el puerto del dominio usando Spring Data JPA y PostgreSQL. */
@Component
@RequiredArgsConstructor
@Transactional
public class PacienteGatewayImpl implements PacienteGateway {

    private final PacienteRepositoryJPA repository;
    private final PacienteMapper mapper;

    @Override
    public Paciente guardar(Paciente paciente) {
        return mapper.toDomain(repository.save(mapper.toEntity(paciente)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Paciente> listarActivos(String nombre, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("codigo"));
        Page<PacienteEntity> resultado = nombre == null
                ? repository.findByActivoTrue(solicitud)
                : repository.buscarActivosPorNombre("%" + nombre.toLowerCase(Locale.ROOT) + "%", solicitud);
        return new Pagina<>(
                resultado.getContent().stream().map(mapper::toDomain).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        return repository.existsByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        return repository.existsByCorreoIgnoreCase(correo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCorreoEnOtroPaciente(String correo, String codigoPaciente) {
        return repository.existsByCorreoIgnoreCaseAndCodigoNot(correo, codigoPaciente);
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
