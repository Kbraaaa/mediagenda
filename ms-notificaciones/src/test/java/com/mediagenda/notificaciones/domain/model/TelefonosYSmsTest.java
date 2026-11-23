package com.mediagenda.notificaciones.domain.model;

import com.mediagenda.notificaciones.Datos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TelefonosYSmsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "3101234567      | +573101234567",
            "310 123 4567    | +573101234567",
            "(310) 123-4567  | +573101234567",
            "573101234567    | +573101234567",
            "+573101234567   | +573101234567",
            "+14155550100    | +14155550100"})
    void normalizaCelularesAlFormatoInternacional(String entrada, String esperado) {
        assertThat(Telefonos.aE164(entrada)).contains(esperado);
    }

    @Test
    void rechazaLoQueNoEsCelular() {
        assertThat(Telefonos.aE164(null)).isEmpty();
        assertThat(Telefonos.aE164("12345")).isEmpty();
        assertThat(Telefonos.aE164("6018765432")).isEmpty();
        assertThat(Telefonos.aE164("+0123")).isEmpty();
    }

    @Test
    void smsCortoSoloParaLosAvisosUrgentes() {
        assertThat(Plantillas.redactarSms(Datos.eventoCita("cita.agendada")).orElseThrow())
                .isEqualTo("MediAgenda: cita CIT-2026-000001 2026-11-21 08:00 agendada con Andrea Lopez.");
        assertThat(Plantillas.redactarSms(Datos.eventoCita("cita.reprogramada")).orElseThrow()).contains("reprogramo");
        assertThat(Plantillas.redactarSms(Datos.eventoCita("cita.cancelada")).orElseThrow()).contains("cancelo");
        assertThat(Plantillas.redactarSms(Datos.eventoCita("cita.recordatorio")).orElseThrow()).contains("recuerde");
        assertThat(Plantillas.redactarSms(Datos.eventoFormula("formula.emitida")).orElseThrow())
                .contains("FOR-2026-000001");
        assertThat(Plantillas.redactarSms(Datos.eventoFormula("formula.dispensada"))).isEmpty();
        assertThat(Plantillas.redactarSms(Datos.eventoCita("cita.triage.completado"))).isEmpty();
    }

    @Test
    void smsSinMedicoYRecortadoA160() {
        EventoRecibido sinMedico = new EventoRecibido("e", "cita.recordatorio", null, null, "a@b.co", null,
                "C".repeat(200), null, null, null, null, null, null, null);

        String sms = Plantillas.redactarSms(sinMedico).orElseThrow();

        assertThat(sms).hasSize(160);
        assertThat(Plantillas.redactarSms(new EventoRecibido("e", "cita.agendada", null, null, "a@b.co", null,
                "C", null, null, null, null, null, null, null)).orElseThrow()).endsWith("con su medico.");
        assertThat(Plantillas.redactarSms(new EventoRecibido("e", null, null, null, "a@b.co", null,
                null, null, null, null, null, null, null, null))).isEmpty();
    }
}
