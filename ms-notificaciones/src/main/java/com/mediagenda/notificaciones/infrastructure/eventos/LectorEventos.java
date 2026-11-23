package com.mediagenda.notificaciones.infrastructure.eventos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import tools.jackson.databind.json.JsonMapper;

/** Convierte el JSON de un evento (venga de RabbitMQ o de SQS) en el modelo del dominio. */
final class LectorEventos {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private LectorEventos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EventoJson(String eventoId, String tipo, String codigoPaciente, String nombrePaciente,
                      String correoPaciente, String telefonoPaciente, String codigoCita, String medico, String especialidad, String fecha,
                      String hora, String codigoFormula, String medicamentos, String detalle) {
    }

    static EventoRecibido leer(String cuerpo) {
        EventoJson e = JSON.readValue(cuerpo, EventoJson.class);
        return new EventoRecibido(e.eventoId(), e.tipo(), e.codigoPaciente(), e.nombrePaciente(),
                e.correoPaciente(), e.telefonoPaciente(), e.codigoCita(), e.medico(), e.especialidad(), e.fecha(), e.hora(),
                e.codigoFormula(), e.medicamentos(), e.detalle());
    }
}
