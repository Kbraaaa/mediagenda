package com.mediagenda.farmacia.domain.model.gateway;

import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoGateway {

    Medicamento guardar(Medicamento medicamento);

    Optional<Medicamento> buscarPorCodigo(String codigo);

    Pagina<Medicamento> listar(String nombre, int pagina, int tamano);

    boolean existe(String nombre, String presentacion);

    /**
     * Lee los medicamentos BLOQUEANDO sus filas (SELECT ... FOR UPDATE) hasta que termine la
     * transaccion: dos dispensaciones simultaneas del mismo medicamento se atienden una tras otra.
     */
    List<Medicamento> bloquear(List<UUID> ids);

    void actualizarStock(UUID idMedicamento, int nuevoStock);

    /** Suma unidades en una sola sentencia (stock = stock + cantidad). */
    void sumarStock(UUID idMedicamento, int cantidad);

    long siguienteConsecutivo();
}
