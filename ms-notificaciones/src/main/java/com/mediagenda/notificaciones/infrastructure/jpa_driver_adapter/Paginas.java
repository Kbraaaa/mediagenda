package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.domain.model.Pagina;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;

import java.util.function.Function;

/** Convierte una Page de Spring Data en la Pagina del dominio. */
@UtilityClass
class Paginas {

    static <E, D> Pagina<D> desde(Page<E> page, Function<E, D> conversion) {
        return new Pagina<>(page.getContent().stream().map(conversion).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
