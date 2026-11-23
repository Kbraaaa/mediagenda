package com.mediagenda.citas.domain.model.exception;

import java.time.LocalDate;

/** 422: la fecha es festivo en Colombia (segun Nager.Date) y la IPS no atiende. */
public class DiaFestivoException extends RuntimeException {

    public DiaFestivoException(LocalDate fecha) {
        super("El " + fecha + " es dia festivo en Colombia: no se programan citas");
    }
}
