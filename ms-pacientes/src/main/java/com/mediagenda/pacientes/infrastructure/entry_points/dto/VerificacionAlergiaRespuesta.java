package com.mediagenda.pacientes.infrastructure.entry_points.dto;

import java.util.List;

public record VerificacionAlergiaRespuesta(String codigoPaciente, String sustancia, boolean alerta,
                                           List<String> coincidencias) {
}
