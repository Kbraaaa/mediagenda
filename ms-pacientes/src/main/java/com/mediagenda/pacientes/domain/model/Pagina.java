package com.mediagenda.pacientes.domain.model;

import java.util.List;

/**
 * Resultado paginado propio del dominio, para que los casos de uso no dependan de Spring Data.
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static final int TAMANO_MAXIMO = 100;

    public static void validarParametros(int pagina, int tamano) {
        if (pagina < 0) {
            throw new IllegalArgumentException("El numero de pagina no puede ser negativo");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }
}
