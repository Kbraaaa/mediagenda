package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.BajaInventario;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.MotivoBaja;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Saca unidades del inventario por vencimiento, deterioro o retiro sanitario (lo contrario del reabastecimiento).
 * Bloquea la fila del medicamento dentro de una transaccion, igual que la dispensacion, para que dos operaciones
 * simultaneas no dejen el stock negativo.
 */
@RequiredArgsConstructor
public class DarDeBajaMedicamentoUseCase {

    private final MedicamentoGateway medicamentos;
    private final TransaccionGateway transaccion;

    public BajaInventario ejecutar(String codigo, Integer cantidad, String motivo) {
        Validaciones.rango(cantidad, "cantidad", 1, 10000);
        MotivoBaja motivoBaja = MotivoBaja.desde(motivo);
        Medicamento encontrado = medicamentos.buscarPorCodigo(codigo).orElseThrow(() ->
                new NoEncontradoException("MEDICAMENTO_NO_ENCONTRADO", "No existe el medicamento " + codigo));
        return transaccion.ejecutar(() -> {
            Medicamento bloqueado = medicamentos.bloquear(List.of(encontrado.getIdMedicamento())).get(0);
            if (bloqueado.getStock() < cantidad) {
                throw new ConflictoException("STOCK_INSUFICIENTE", "No se pueden dar de baja " + cantidad
                        + " unidades de " + bloqueado.getNombre() + ": solo hay " + bloqueado.getStock());
            }
            int nuevoStock = bloqueado.getStock() - cantidad;
            medicamentos.actualizarStock(bloqueado.getIdMedicamento(), nuevoStock);
            bloqueado.setStock(nuevoStock);
            return new BajaInventario(bloqueado, cantidad, motivoBaja);
        });
    }
}
