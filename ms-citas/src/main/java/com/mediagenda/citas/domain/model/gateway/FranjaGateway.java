package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.model.Franja;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.Pagina;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface FranjaGateway {

    List<Franja> guardarTodas(List<Franja> franjas);

    Optional<Franja> buscarPorId(Long idFranja);

    Pagina<Franja> listar(FiltroFranjas filtro, int pagina, int tamano);

    boolean existeSolapamiento(Medico medico, LocalDate fecha, LocalTime inicio, LocalTime fin);

    /**
     * Reserva la franja solo si sigue DISPONIBLE, en una sola sentencia UPDATE atomica.
     * Devuelve false si otro paciente la tomo primero (asi se resuelve la concurrencia).
     */
    boolean reservar(Long idFranja);

    void liberar(Long idFranja);
}
