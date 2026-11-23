package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class MedicamentoGatewayImpl implements MedicamentoGateway {

    private final MedicamentoRepositoryJPA repository;
    private final EntidadesMapper mapper;

    @Override
    public Medicamento guardar(Medicamento medicamento) {
        return mapper.toDomain(repository.save(mapper.toEntity(medicamento)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicamento> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Medicamento> listar(String nombre, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("nombre"));
        var resultado = nombre == null
                ? repository.findByActivoTrue(solicitud)
                : repository.findByActivoTrueAndNombreContainingIgnoreCase(nombre, solicitud);
        return Paginas.desde(resultado, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(String nombre, String presentacion) {
        return repository.existsByNombreIgnoreCaseAndPresentacionIgnoreCase(nombre, presentacion);
    }

    @Override
    public List<Medicamento> bloquear(List<UUID> ids) {
        return repository.bloquear(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void actualizarStock(UUID idMedicamento, int nuevoStock) {
        repository.actualizarStock(idMedicamento, nuevoStock);
    }

    @Override
    public void sumarStock(UUID idMedicamento, int cantidad) {
        repository.sumarStock(idMedicamento, cantidad);
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
