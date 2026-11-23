package com.mediagenda.farmacia.domain.model.gateway;

import java.util.function.Supplier;

/**
 * Ejecuta un bloque como UNA transaccion de base de datos: o se aplica todo (descontar stock y
 * marcar la formula) o nada. El dominio no sabe que por debajo es Spring/JPA.
 */
public interface TransaccionGateway {

    <T> T ejecutar(Supplier<T> bloque);
}
