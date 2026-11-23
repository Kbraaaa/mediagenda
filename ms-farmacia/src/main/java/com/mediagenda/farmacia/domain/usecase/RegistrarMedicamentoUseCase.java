package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Codigos;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Registra un medicamento en el inventario con su codigo MDT-0001. */
@RequiredArgsConstructor
public class RegistrarMedicamentoUseCase {

    private final MedicamentoGateway medicamentos;

    public Medicamento ejecutar(Medicamento datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del medicamento son obligatorios");
        }
        Validaciones.texto(datos.getNombre(), "nombre", 3, 120);
        Validaciones.texto(datos.getPresentacion(), "presentacion", 3, 80);
        Validaciones.rango(datos.getStock(), "stock", 0, 100000);
        String nombre = datos.getNombre().trim();
        String presentacion = datos.getPresentacion().trim();
        if (medicamentos.existe(nombre, presentacion)) {
            throw new ConflictoException("MEDICAMENTO_DUPLICADO",
                    "Ya existe " + nombre + " en presentacion " + presentacion);
        }
        return medicamentos.guardar(datos.toBuilder()
                .idMedicamento(UUID.randomUUID())
                .codigo(Codigos.medicamento(medicamentos.siguienteConsecutivo()))
                .nombre(nombre)
                .presentacion(presentacion)
                .activo(true)
                .build());
    }
}
