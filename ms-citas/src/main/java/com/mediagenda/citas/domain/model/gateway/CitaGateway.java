package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.Cita;
import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.model.EstadoTriage;
import com.mediagenda.citas.domain.model.Pagina;
import com.mediagenda.citas.domain.model.ResultadoTriage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaGateway {

    Cita guardar(Cita cita);

    Optional<Cita> buscarPorCodigo(String codigo);

    Pagina<Cita> listar(FiltroCitas filtro, int pagina, int tamano);

    /** Citas AGENDADA cuyo inicio esta entre desde y hasta y aun no tienen recordatorio. */
    List<Cita> pendientesDeRecordatorio(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Actualiza solo las columnas del triage (no toda la cita), para no pisar un cambio de estado
     * que haya ocurrido mientras la IA respondia. resultado es null cuando el triage fallo.
     */
    void actualizarTriage(String codigo, EstadoTriage estado, ResultadoTriage resultado);

    void marcarRecordatorioEnviado(String codigo);

    long siguienteConsecutivo();
}
