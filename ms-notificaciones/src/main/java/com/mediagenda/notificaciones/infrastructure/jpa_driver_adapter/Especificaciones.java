package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Especificaciones {

    public static Specification<NotificacionEntity> notificaciones(FiltroNotificaciones filtro) {
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
