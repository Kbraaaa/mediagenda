package com.mediagenda.pacientes.infrastructure.entry_points.dto;

public record CompatibilidadRespuesta(String codigoReceptor, String tipoSangreReceptor,
                                      String codigoDonante, String tipoSangreDonante,
                                      boolean compatible, String explicacion) {
}
