package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class FormulaGatewayImpl implements FormulaGateway {

    private final FormulaRepositoryJPA repository;
    private final MedicamentoRepositoryJPA medicamentoRepository;
    private final EntidadesMapper mapper;

    @Override
    public Formula guardar(Formula formula) {
        FormulaEntity entidad = mapper.toEntity(formula, medicamentoRepository::getReferenceById);
        return mapper.toDomain(repository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Formula> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public Optional<Formula> bloquearPorCodigo(String codigo) {
        return repository.bloquearPorCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Formula> listar(FiltroFormulas filtro, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaEmision"));
        return Paginas.desde(repository.findAll(Especificaciones.formulas(filtro), solicitud), mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeActivaParaCita(String codigoCita) {
        return repository.existsByCodigoCitaAndEstadoNot(codigoCita, EstadoFormula.ANULADA);
    }

    @Override
    public void actualizarEstado(String codigo, EstadoFormula estado, LocalDateTime fechaDispensacion) {
        repository.actualizarEstado(codigo, estado, fechaDispensacion);
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
