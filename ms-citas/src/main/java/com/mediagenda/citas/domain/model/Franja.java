package com.mediagenda.citas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Espacio de la agenda de un medico (por ejemplo, lunes 24 de 8:00 a 8:30). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Franja {

    private Long idFranja;
    private Medico medico;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoFranja estado;

    public LocalDateTime inicio() {
        return LocalDateTime.of(fecha, horaInicio);
    }
}
