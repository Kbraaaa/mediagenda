package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.ResultadoTriage;

/** Analiza los sintomas (API externa de IA: Gemini). Lanza TriageException si falla. */
public interface TriageGateway {

    ResultadoTriage analizar(String sintomas);
}
