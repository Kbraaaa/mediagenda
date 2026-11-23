package com.mediagenda.citas.domain.model.gateway;

import java.time.LocalDate;

/** Festivos de Colombia (API externa Nager.Date, con cache). */
public interface FestivoGateway {

    boolean esFestivo(LocalDate fecha);
}
