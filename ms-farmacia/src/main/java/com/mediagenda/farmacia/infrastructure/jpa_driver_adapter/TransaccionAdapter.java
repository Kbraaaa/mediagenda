package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Implementa la transaccion del dominio con el TransactionTemplate de Spring. */
public class TransaccionAdapter implements TransaccionGateway {

    private final TransactionTemplate plantilla;

    public TransaccionAdapter(TransactionTemplate plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public <T> T ejecutar(Supplier<T> bloque) {
        return plantilla.execute(estado -> bloque.get());
    }
}
