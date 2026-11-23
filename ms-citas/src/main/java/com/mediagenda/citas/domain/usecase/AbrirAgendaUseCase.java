package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.EstadoFranja;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.SolicitudFranjas;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.gateway.FranjaGateway;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abre la agenda de un medico para un dia: parte el rango horaInicio-horaFin en franjas de
 * duracionMinutos. No permite dias pasados, festivos ni rangos que se crucen con franjas existentes.
 */
@RequiredArgsConstructor
public class AbrirAgendaUseCase {

    public static final int DURACION_MINIMA = 10;
    public static final int DURACION_MAXIMA = 120;
    public static final int MAXIMO_FRANJAS = 48;

    private final MedicoGateway medicoGateway;
    private final FranjaGateway franjaGateway;
    private final ReglasAgenda reglas;

    public List<Franja> ejecutar(String codigoMedico, SolicitudFranjas solicitud) {
        validar(solicitud);
        Medico medico = medicoGateway.buscarPorCodigo(codigoMedico)
                .orElseThrow(() -> new NoEncontradoException("MEDICO_NO_ENCONTRADO", "No existe el medico " + codigoMedico));
        if (!medico.isActivo()) {
            throw new ConflictoException("MEDICO_INACTIVO", "El medico " + codigoMedico + " no esta activo");
        }
        if (solicitud.fecha().isBefore(reglas.hoy())) {
            throw new IllegalArgumentException("No se puede abrir agenda en una fecha pasada");
        }
        reglas.validarNoFestivo(solicitud.fecha());

        List<Franja> franjas = new ArrayList<>();
        LocalTime inicio = solicitud.horaInicio();
        LocalTime fin = inicio.plusMinutes(solicitud.duracionMinutos());
        // fin.isAfter(inicio) evita dar la vuelta a la medianoche (23:50 + 30 min = 00:20)
        while (fin.isAfter(inicio) && !fin.isAfter(solicitud.horaFin()) && franjas.size() < MAXIMO_FRANJAS) {
            franjas.add(Franja.builder()
                    .medico(medico)
                    .fecha(solicitud.fecha())
                    .horaInicio(inicio)
                    .horaFin(fin)
                    .estado(EstadoFranja.DISPONIBLE)
                    .build());
            inicio = fin;
            fin = inicio.plusMinutes(solicitud.duracionMinutos());
        }
        if (franjas.isEmpty()) {
            throw new IllegalArgumentException("El rango de horas no alcanza para una franja de "
                    + solicitud.duracionMinutos() + " minutos");
        }
        LocalTime finReal = franjas.get(franjas.size() - 1).getHoraFin();
        if (franjaGateway.existeSolapamiento(medico, solicitud.fecha(), solicitud.horaInicio(), finReal)) {
            throw new ConflictoException("AGENDA_SOLAPADA",
                    "El medico ya tiene franjas en ese horario del " + solicitud.fecha());
        }
        return franjaGateway.guardarTodas(franjas);
    }

    private static void validar(SolicitudFranjas s) {
        if (s == null || s.fecha() == null || s.horaInicio() == null || s.horaFin() == null) {
            throw new IllegalArgumentException("fecha, horaInicio y horaFin son obligatorios");
        }
        if (!s.horaFin().isAfter(s.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (s.duracionMinutos() < DURACION_MINIMA || s.duracionMinutos() > DURACION_MAXIMA) {
            throw new IllegalArgumentException("La duracion debe estar entre " + DURACION_MINIMA
                    + " y " + DURACION_MAXIMA + " minutos");
        }
    }
}
