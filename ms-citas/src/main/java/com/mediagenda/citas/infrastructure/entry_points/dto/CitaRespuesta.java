package com.mediagenda.citas.infrastructure.entry_points.dto;

import java.time.LocalDateTime;

public record CitaRespuesta(String codigo, String codigoPaciente, String nombrePaciente, String correoPaciente,
                            String telefonoPaciente,
                            String estado,
                            FranjaRespuesta franja, String sintomas, TriageRespuesta triage,
                            LocalDateTime fechaCreacion) {
}
