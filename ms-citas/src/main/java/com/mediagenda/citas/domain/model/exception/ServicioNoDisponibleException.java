package com.mediagenda.citas.domain.model.exception;

/** 503: un servicio del que dependemos (ms-pacientes) no respondio a tiempo. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
