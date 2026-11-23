package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Filtros dinamicos del listado de formulas. */
@UtilityClass
public class Especificaciones {

    public static Specification<FormulaEntity> formulas(FiltroFormulas filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.codigoPaciente() != null) {
                condiciones.add(cb.equal(root.get("codigoPaciente"), filtro.codigoPaciente()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
