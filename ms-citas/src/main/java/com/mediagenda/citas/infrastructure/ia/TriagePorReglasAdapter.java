package com.mediagenda.citas.infrastructure.ia;

import com.mediagenda.citas.domain.model.Especialidad;
import com.mediagenda.citas.domain.model.Prioridad;
import com.mediagenda.citas.domain.model.ResultadoTriage;
import com.mediagenda.citas.domain.model.gateway.TriageGateway;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Triage de respaldo por palabras clave. Se usa cuando no hay clave de Gemini o cuando se desactiva
 * la IA (por ejemplo en las pruebas de carga, para no gastar la cuota gratuita).
 */
public class TriagePorReglasAdapter implements TriageGateway {

    private static final List<String> SENALES_DE_ALARMA = List.of(
            "dolor en el pecho", "dolor de pecho", "falta de aire", "no puedo respirar", "desmayo",
            "convulsion", "sangrado abundante", "perdida de conciencia", "paralisis");

    private static final Map<String, Especialidad> PALABRAS = new LinkedHashMap<>();

    static {
        PALABRAS.put("pecho", Especialidad.CARDIOLOGIA);
        PALABRAS.put("corazon", Especialidad.CARDIOLOGIA);
        PALABRAS.put("palpitacion", Especialidad.CARDIOLOGIA);
        PALABRAS.put("piel", Especialidad.DERMATOLOGIA);
        PALABRAS.put("brote", Especialidad.DERMATOLOGIA);
        PALABRAS.put("bebe", Especialidad.PEDIATRIA);
        PALABRAS.put("nino", Especialidad.PEDIATRIA);
        PALABRAS.put("embarazo", Especialidad.GINECOLOGIA);
        PALABRAS.put("hueso", Especialidad.ORTOPEDIA);
        PALABRAS.put("rodilla", Especialidad.ORTOPEDIA);
        PALABRAS.put("espalda", Especialidad.ORTOPEDIA);
        PALABRAS.put("cabeza", Especialidad.NEUROLOGIA);
        PALABRAS.put("migrana", Especialidad.NEUROLOGIA);
        PALABRAS.put("ansiedad", Especialidad.PSIQUIATRIA);
        PALABRAS.put("tristeza", Especialidad.PSIQUIATRIA);
        PALABRAS.put("ojo", Especialidad.OFTALMOLOGIA);
        PALABRAS.put("vista", Especialidad.OFTALMOLOGIA);
        PALABRAS.put("oido", Especialidad.OTORRINOLARINGOLOGIA);
        PALABRAS.put("garganta", Especialidad.OTORRINOLARINGOLOGIA);
        PALABRAS.put("estomago", Especialidad.GASTROENTEROLOGIA);
        PALABRAS.put("diarrea", Especialidad.GASTROENTEROLOGIA);
        PALABRAS.put("tos", Especialidad.NEUMOLOGIA);
        PALABRAS.put("respirar", Especialidad.NEUMOLOGIA);
    }

    @Override
    public ResultadoTriage analizar(String sintomas) {
        String texto = normalizar(sintomas);
        boolean alarma = SENALES_DE_ALARMA.stream().anyMatch(texto::contains);
        Especialidad especialidad = PALABRAS.entrySet().stream()
                .filter(e -> texto.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Especialidad.MEDICINA_GENERAL);
        Prioridad prioridad = alarma ? Prioridad.ALTA
                : especialidad == Especialidad.MEDICINA_GENERAL ? Prioridad.BAJA : Prioridad.MEDIA;
        return new ResultadoTriage(especialidad, prioridad, "Clasificacion por palabras clave (sin IA)");
    }

    /** Minusculas y sin tildes, para que "Corazón" y "corazon" coincidan. */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
