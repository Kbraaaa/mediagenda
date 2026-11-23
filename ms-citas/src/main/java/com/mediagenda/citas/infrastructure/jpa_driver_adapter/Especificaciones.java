package com.mediagenda.citas.infrastructure.jpa_driver_adapter;

import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.FiltroFranjas;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Filtros dinamicos de los listados: solo se agrega la condicion de los filtros que llegaron. */
@UtilityClass
public class Especificaciones {

    public static Specification<CitaEntity> citas(FiltroCitas filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.codigoPaciente() != null) {
                condiciones.add(cb.equal(root.get("codigoPaciente"), filtro.codigoPaciente()));
            }
            if (filtro.codigoMedico() != null) {
                condiciones.add(cb.equal(root.get("franja").get("medico").get("codigo"), filtro.codigoMedico()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    public static Specification<FranjaEntity> franjas(FiltroFranjas filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.codigoMedico() != null) {
                condiciones.add(cb.equal(root.get("medico").get("codigo"), filtro.codigoMedico()));
            }
            if (filtro.fecha() != null) {
                condiciones.add(cb.equal(root.get("fecha"), filtro.fecha()));
            }
            if (filtro.soloDisponibles()) {
                condiciones.add(cb.equal(root.get("estado"), EstadoFranja.DISPONIBLE));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
