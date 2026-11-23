package com.mediagenda.citas.domain.model.exception;

/** La IA no respondio o respondio algo invalido. No llega al usuario: la cita queda con triage FALLIDO. */
public class TriageException extends RuntimeException {

    public TriageException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
