package com.mediagenda.farmacia.infrastructure.entry_points.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FormulaRespuesta(String codigo, String codigoCita, String codigoPaciente, String nombrePaciente,
                               String estado, LocalDateTime fechaEmision, LocalDateTime fechaDispensacion,
                               List<ItemRespuesta> items) {
}
