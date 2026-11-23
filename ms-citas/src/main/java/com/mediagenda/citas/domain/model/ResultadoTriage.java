package com.mediagenda.citas.domain.model;

/** Respuesta del triage: especialidad sugerida, prioridad y el porque. Es orientativo, no un diagnostico. */
public record ResultadoTriage(Especialidad especialidad, Prioridad prioridad, String motivo) {
}
