package com.mediagenda.pacientes.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas de dominio de alergias y compatibilidad sanguinea. */
class ReglasClinicasTest {

    // ---------- Alergias ----------

    @Test
    void separaPorComaOPuntoYComaYDescartaVacios() {
        assertThat(Alergias.separar("Penicilina, Ibuprofeno; ;Sulfas")).containsExactly("Penicilina", "Ibuprofeno", "Sulfas");
        assertThat(Alergias.separar(null)).isEmpty();
        assertThat(Alergias.separar("  ")).isEmpty();
    }

    @Test
    void normalizaSinTildesNiMayusculas() {
        assertThat(Alergias.normalizar("  Ácido   Acetilsalicílico ")).isEqualTo("acido acetilsalicilico");
    }

    @Test
    void reconoceAlergiaYaRegistrada() {
        assertThat(Alergias.contiene("Penicilina, Ibuprofeno", "IBUPROFENO")).isTrue();
        assertThat(Alergias.contiene("Penicilina", "Sulfas")).isFalse();
    }

    @Test
    void coincidenciaPorMismaSustanciaPorFamiliaONinguna() {
        assertThat(Alergias.coincidencia("Penicilina", "penicilina benzatinica")).contains("Penicilina (misma sustancia)");
        assertThat(Alergias.coincidencia("Penicilina", "Amoxicilina"))
                .contains("Penicilina (misma familia: penicilinas)");
        assertThat(Alergias.coincidencia("Ibuprofeno", "Naproxeno")).get().asString().contains("AINE");
        assertThat(Alergias.coincidencia("Penicilina", "Loratadina")).isEmpty();
        assertThat(Alergias.coincidencia("Polen", "Amoxicilina")).isEmpty();
    }

    // ---------- Compatibilidad sanguinea ----------

    @ParameterizedTest(name = "receptor {0} recibe de {1}: {2}")
    @CsvSource({
            "AB+, O-, true", "AB+, A+, true", "AB+, B-, true",
            "O-, O-, true", "O-, O+, false", "O+, A+, false",
            "A+, O+, true", "A-, A+, false", "B+, A+, false", "A-, AB-, false", "B-, O-, true"
    })
    void aplicaLasReglasAboYRh(String receptor, String donante, boolean esperado) {
        CompatibilidadSanguinea c = CompatibilidadSanguinea.evaluar("PAC-000001", receptor, "PAC-000002", donante);
        assertThat(c.compatible()).isEqualTo(esperado);
        assertThat(c.explicacion()).isNotBlank();
    }

    @Test
    void explicaElMotivoDelRechazo() {
        assertThat(CompatibilidadSanguinea.evaluar("R", "O-", "D", "O+").explicacion()).contains("Rh negativo");
        assertThat(CompatibilidadSanguinea.evaluar("R", "B+", "D", "A+").explicacion()).contains("anticuerpos");
        assertThat(CompatibilidadSanguinea.evaluar("R", "A+", "D", "O-").explicacion()).contains("puede recibir");
    }
}
