package com.mediagenda.farmacia.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Formula medica emitida a partir de una cita ATENDIDA. Codigo legible: FOR-2026-000012. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Formula {

    private UUID idFormula;
    private String codigo;
    private String codigoCita;
    private String codigoPaciente;
    private String nombrePaciente;
    private String correoPaciente;
    private String telefonoPaciente;
    private EstadoFormula estado;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaDispensacion;
    private List<ItemFormula> items;
}
