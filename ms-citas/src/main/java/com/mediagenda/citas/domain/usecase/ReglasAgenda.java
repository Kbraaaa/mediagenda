package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.EstadoCita;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.DiaFestivoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.CitaGateway;
import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reglas de agenda que comparten varios casos de uso. El reloj se inyecta (zona America/Bogota)
 * para que "ahora" sea la hora de Colombia aunque el servidor de AWS este en UTC, y para poder probarlo.
 */
@RequiredArgsConstructor
public class ReglasAgenda {

    private final Clock reloj;
    private final FestivoGateway festivoGateway;

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Una franja se puede reservar si no ha pasado, el medico esta activo y no es festivo. */
    public void validarFranjaReservable(Franja franja) {
        if (!franja.inicio().isAfter(ahora())) {
            throw new ConflictoException("FRANJA_VENCIDA", "La franja " + franja.getIdFranja() + " ya paso");
        }
        if (!franja.getMedico().isActivo()) {
            throw new ConflictoException("MEDICO_INACTIVO", "El medico de esta franja no esta activo");
        }
        validarNoFestivo(franja.getFecha());
    }

    public void validarNoFestivo(LocalDate fecha) {
        if (festivoGateway.esFestivo(fecha)) {
            throw new DiaFestivoException(fecha);
        }
    }

    /** Busca la cita y exige que siga AGENDADA (no se puede cancelar o mover una cita ya cerrada). */
    public static Cita buscarAgendada(CitaGateway citaGateway, String codigo) {
        Cita cita = citaGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new NoEncontradoException("CITA_NO_ENCONTRADA", "No existe la cita " + codigo));
        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new ConflictoException("CITA_NO_AGENDADA",
                    "La cita " + codigo + " esta " + cita.getEstado() + " y ya no se puede modificar");
        }
        return cita;
    }

    public static void validarTexto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        int largo = valor.trim().length();
        if (largo < minimo || largo > maximo) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
    }
}
