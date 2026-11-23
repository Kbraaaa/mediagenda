package com.mediagenda.farmacia.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Medicamento del inventario de la farmacia. Codigo legible: MDT-0001. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    private UUID idMedicamento;
    private String codigo;
    private String nombre;
    private String presentacion;
    private int stock;
    private boolean activo;
}
