package com.mediagenda.pacientes.domain.model;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reglas sobre la lista de alergias del paciente. Se guardan como texto separado por comas
 * ("Penicilina, Ibuprofeno"); aqui se separan, se comparan sin mayusculas ni tildes y se
 * reconocen algunas familias de medicamentos con reaccion cruzada conocida.
 */
@UtilityClass
public class Alergias {

    /** Familias de reaccion cruzada: alergia a uno de ellos alerta sobre los demas. */
    private static final Map<String, List<String>> FAMILIAS = Map.of(
            "penicilinas", List.of("penicilina", "amoxicilina", "ampicilina", "dicloxacilina", "oxacilina"),
            "sulfonamidas", List.of("sulfa", "sulfas", "sulfametoxazol", "sulfadiazina"),
            "antiinflamatorios no esteroideos (AINE)",
            List.of("ibuprofeno", "naproxeno", "diclofenaco", "aspirina", "acido acetilsalicilico", "ketorolaco"));

    public static List<String> separar(String alergias) {
        if (alergias == null || alergias.isBlank()) {
            return List.of();
        }
        return Arrays.stream(alergias.split("[,;]"))
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .toList();
    }

    /** Minusculas, sin tildes y sin espacios repetidos: "Ácido  Acetilsalicílico" -> "acido acetilsalicilico". */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static boolean contiene(String alergias, String alergia) {
        String buscada = normalizar(alergia);
        return separar(alergias).stream().anyMatch(a -> normalizar(a).equals(buscada));
    }

    /**
     * Explica por que la sustancia es riesgosa para alguien alergico a {@code alergia}:
     * misma sustancia, una contiene a la otra ("penicilina benzatinica") o misma familia.
     */
    public static Optional<String> coincidencia(String alergia, String sustancia) {
        String a = normalizar(alergia);
        String s = normalizar(sustancia);
        if (a.equals(s) || a.contains(s) || s.contains(a)) {
            return Optional.of(alergia + " (misma sustancia)");
        }
        return FAMILIAS.entrySet().stream()
                .filter(f -> f.getValue().stream().anyMatch(m -> a.contains(m))
                        && f.getValue().stream().anyMatch(m -> s.contains(m)))
                .map(f -> alergia + " (misma familia: " + f.getKey() + ")")
                .findFirst();
    }
}
