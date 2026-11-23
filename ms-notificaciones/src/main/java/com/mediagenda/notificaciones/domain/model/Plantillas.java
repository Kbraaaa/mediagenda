package com.mediagenda.notificaciones.domain.model;

import lombok.experimental.UtilityClass;

import java.util.Optional;

/** Redacta el correo (y el SMS, cuando aplica) segun el tipo de evento. */
@UtilityClass
public class Plantillas {

    public static Aviso redactar(EventoRecibido e) {
        String saludo = "Hola " + texto(e.nombrePaciente(), "paciente") + ",\n\n";
        String cita = "su cita " + texto(e.codigoCita(), "") + " con " + texto(e.medico(), "el medico")
                + " (" + texto(e.especialidad(), "") + ") el " + texto(e.fecha(), "") + " a las " + hora(e.hora());
        String firma = "\n\nMediAgenda IPS";
        String tipo = texto(e.tipo(), "");
        return switch (tipo) {
            case "cita.agendada" -> new Aviso("Cita agendada " + texto(e.codigoCita(), ""),
                    saludo + "Quedo agendada " + cita + "." + firma);
            case "cita.reprogramada" -> new Aviso("Cita reprogramada " + texto(e.codigoCita(), ""),
                    saludo + "Reprogramamos " + cita + "." + firma);
            case "cita.cancelada" -> new Aviso("Cita cancelada " + texto(e.codigoCita(), ""),
                    saludo + "Se cancelo " + cita + "." + firma);
            case "cita.recordatorio" -> new Aviso("Recordatorio: su cita es pronto",
                    saludo + "Le recordamos " + cita + "." + firma);
            case "cita.triage.completado" -> new Aviso("Orientacion sobre su cita " + texto(e.codigoCita(), ""),
                    saludo + texto(e.detalle(), "") + firma);
            case "formula.emitida" -> new Aviso("Formula " + texto(e.codigoFormula(), "") + " lista",
                    saludo + texto(e.detalle(), "Su formula esta lista") + ".\nMedicamentos: "
                            + texto(e.medicamentos(), "") + firma);
            case "formula.dispensada" -> new Aviso("Medicamentos entregados",
                    saludo + texto(e.detalle(), "Sus medicamentos fueron entregados") + ".\nMedicamentos: "
                            + texto(e.medicamentos(), "") + firma);
            default -> new Aviso("Aviso de MediAgenda", saludo + texto(e.detalle(), "Tiene un nuevo aviso.") + firma);
        };
    }

    /**
     * Texto corto para SMS (maximo 160 caracteres). Solo para los avisos que no pueden esperar:
     * cambios de la cita, recordatorio y formula lista. El resto va solo por correo.
     */
    public static Optional<String> redactarSms(EventoRecibido e) {
        String cita = texto(e.codigoCita(), "") + " " + texto(e.fecha(), "") + " " + hora(e.hora());
        String mensaje = switch (texto(e.tipo(), "")) {
            case "cita.agendada" -> "MediAgenda: cita " + cita + " agendada con " + texto(e.medico(), "su medico") + ".";
            case "cita.reprogramada" -> "MediAgenda: su cita se reprogramo para " + cita + ".";
            case "cita.cancelada" -> "MediAgenda: se cancelo su cita " + cita + ".";
            case "cita.recordatorio" -> "MediAgenda: recuerde su cita " + cita + " con " + texto(e.medico(), "su medico") + ".";
            case "formula.emitida" -> "MediAgenda: su formula " + texto(e.codigoFormula(), "") + " esta lista para reclamar.";
            default -> null;
        };
        return Optional.ofNullable(mensaje).map(m -> m.length() > 160 ? m.substring(0, 160) : m);
    }

    private static String texto(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    /** "08:00:00" -> "08:00". */
    private static String hora(String valor) {
        String h = texto(valor, "");
        return h.length() >= 5 ? h.substring(0, 5) : h;
    }
}
