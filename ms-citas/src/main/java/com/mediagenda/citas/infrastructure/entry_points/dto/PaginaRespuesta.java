package com.mediagenda.citas.infrastructure.entry_points.dto;

import java.util.List;

public record PaginaRespuesta<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
