package com.mediagenda.pacientes.domain.model;

/**
 * Compatibilidad de globulos rojos entre donante y receptor (sistemas ABO y Rh).
 * El donante es compatible si el receptor tiene todos los antigenos A/B del donante
 * y, si el donante es Rh+, el receptor tambien lo es. O- es donante universal y AB+ receptor universal.
 */
public record CompatibilidadSanguinea(String codigoReceptor, String tipoReceptor,
                                      String codigoDonante, String tipoDonante,
                                      boolean compatible, String explicacion) {

    public static CompatibilidadSanguinea evaluar(String codigoReceptor, String tipoReceptor,
                                                  String codigoDonante, String tipoDonante) {
        String aboReceptor = abo(tipoReceptor);
        String aboDonante = abo(tipoDonante);
        boolean rhReceptor = tipoReceptor.endsWith("+");
        boolean rhDonante = tipoDonante.endsWith("+");
        boolean aboCompatible = aboReceptor.contains("A") || !aboDonante.contains("A");
        aboCompatible = aboCompatible && (aboReceptor.contains("B") || !aboDonante.contains("B"));
        boolean rhCompatible = rhReceptor || !rhDonante;
        String explicacion;
        if (!aboCompatible) {
            explicacion = "El receptor " + tipoReceptor + " tiene anticuerpos contra el grupo " + aboDonante
                    + " del donante";
        } else if (!rhCompatible) {
            explicacion = "Un receptor Rh negativo no puede recibir sangre Rh positiva";
        } else {
            explicacion = "El receptor " + tipoReceptor + " puede recibir globulos rojos " + tipoDonante;
        }
        return new CompatibilidadSanguinea(codigoReceptor, tipoReceptor, codigoDonante, tipoDonante,
                aboCompatible && rhCompatible, explicacion);
    }

    /** "AB+" -> "AB", "O-" -> "" (el grupo O no tiene antigenos A ni B). */
    private static String abo(String tipo) {
        String grupo = tipo.substring(0, tipo.length() - 1);
        return "O".equals(grupo) ? "" : grupo;
    }
}
