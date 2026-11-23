package com.mediagenda.citas.infrastructure.entry_points.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record FranjaRespuesta(Long idFranja, String codigoMedico, String medico, String especialidad,
                              LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String estado) {
}
