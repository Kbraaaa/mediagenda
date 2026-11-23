package com.mediagenda.citas.domain.model.gateway;

/** Pide el triage en segundo plano, para que agendar responda en menos de 500 ms. */
public interface SolicitudTriageGateway {

    void solicitar(String codigoCita);
}
