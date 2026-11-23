package com.mediagenda.farmacia.infrastructure.mapper;

import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.ItemFormula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.infrastructure.jpa_driver_adapter.FormulaEntity;
import com.mediagenda.farmacia.infrastructure.jpa_driver_adapter.ItemFormulaEntity;
import com.mediagenda.farmacia.infrastructure.jpa_driver_adapter.MedicamentoEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

/** Convierte entre el dominio y las entidades JPA. */
@Component
public class EntidadesMapper {

    public Medicamento toDomain(MedicamentoEntity e) {
        return Medicamento.builder()
                .idMedicamento(e.getIdMedicamento())
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .presentacion(e.getPresentacion())
                .stock(e.getStock())
                .activo(e.isActivo())
                .build();
    }

    public MedicamentoEntity toEntity(Medicamento m) {
        MedicamentoEntity e = new MedicamentoEntity();
        e.setIdMedicamento(m.getIdMedicamento());
        e.setCodigo(m.getCodigo());
        e.setNombre(m.getNombre());
        e.setPresentacion(m.getPresentacion());
        e.setStock(m.getStock());
        e.setActivo(m.isActivo());
        return e;
    }

    public Formula toDomain(FormulaEntity e) {
        return Formula.builder()
                .idFormula(e.getIdFormula())
                .codigo(e.getCodigo())
                .codigoCita(e.getCodigoCita())
                .codigoPaciente(e.getCodigoPaciente())
                .nombrePaciente(e.getNombrePaciente())
                .correoPaciente(e.getCorreoPaciente())
                .telefonoPaciente(e.getTelefonoPaciente())
                .estado(e.getEstado())
                .fechaEmision(e.getFechaEmision())
                .fechaDispensacion(e.getFechaDispensacion())
                .items(e.getItems().stream()
                        .map(i -> new ItemFormula(i.getIdItem(), toDomain(i.getMedicamento()), i.getCantidad(),
                                i.getIndicaciones()))
                        .toList())
                .build();
    }

    /** referencia: entrega la entidad JPA (o un proxy) del medicamento por su id. */
    public FormulaEntity toEntity(Formula f, Function<UUID, MedicamentoEntity> referencia) {
        FormulaEntity e = new FormulaEntity();
        e.setIdFormula(f.getIdFormula());
        e.setCodigo(f.getCodigo());
        e.setCodigoCita(f.getCodigoCita());
        e.setCodigoPaciente(f.getCodigoPaciente());
        e.setNombrePaciente(f.getNombrePaciente());
        e.setCorreoPaciente(f.getCorreoPaciente());
        e.setTelefonoPaciente(f.getTelefonoPaciente());
        e.setEstado(f.getEstado());
        e.setFechaEmision(f.getFechaEmision());
        e.setFechaDispensacion(f.getFechaDispensacion());
        for (ItemFormula item : f.getItems()) {
            ItemFormulaEntity ie = new ItemFormulaEntity();
            ie.setIdItem(item.idItem());
            ie.setFormula(e);
            ie.setMedicamento(referencia.apply(item.medicamento().getIdMedicamento()));
            ie.setCantidad(item.cantidad());
            ie.setIndicaciones(item.indicaciones());
            e.getItems().add(ie);
        }
        return e;
    }
}
