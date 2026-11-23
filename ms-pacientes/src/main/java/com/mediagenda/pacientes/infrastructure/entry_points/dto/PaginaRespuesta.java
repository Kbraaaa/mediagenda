package com.mediagenda.pacientes.infrastructure.entry_points.dto;

import java.util.List;

/** Formato de respuesta de los listados paginados. */
public record PaginaRespuesta<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
