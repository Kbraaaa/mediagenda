package com.mediagenda.citas.infrastructure.entry_points.dto;

/** Resultado del triage con IA dentro de la cita. Mientras la IA responde, estado = PENDIENTE. */
public record TriageRespuesta(String estado, String especialidadSugerida, String prioridad, String motivo) {
}
