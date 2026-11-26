package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.BajaInventario;
import com.mediagenda.farmacia.domain.model.DisponibilidadFormula;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.MotivoBaja;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Baja de inventario y verificacion de disponibilidad de una formula. */
class OperacionesFarmaciaUseCaseTest {

    private MedicamentoGateway medicamentos;
    private FormulaGateway formulas;
    private DarDeBajaMedicamentoUseCase baja;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        medicamentos = mock(MedicamentoGateway.class);
        formulas = mock(FormulaGateway.class);
        TransaccionGateway transaccion = mock(TransaccionGateway.class);
        when(transaccion.ejecutar(any())).thenAnswer(i -> ((Supplier<Object>) i.getArgument(0)).get());
        baja = new DarDeBajaMedicamentoUseCase(medicamentos, transaccion);
        when(medicamentos.buscarPorCodigo("MDT-0001")).thenReturn(Optional.of(Datos.acetaminofen()));
        when(medicamentos.bloquear(List.of(Datos.ID_ACETAMINOFEN))).thenReturn(List.of(Datos.acetaminofen()));
    }

    // ---------- Baja de inventario ----------

    @Test
    void daDeBajaUnidadesBloqueandoLaFila() {
        BajaInventario r = baja.ejecutar("MDT-0001", 20, " vencimiento ");

        assertThat(r.cantidad()).isEqualTo(20);
        assertThat(r.motivo()).isEqualTo(MotivoBaja.VENCIMIENTO);
        assertThat(r.medicamento().getStock()).isEqualTo(480);
        verify(medicamentos).bloquear(List.of(Datos.ID_ACETAMINOFEN));
        verify(medicamentos).actualizarStock(Datos.ID_ACETAMINOFEN, 480);
    }

    @Test
    void puedeDejarElStockEnCero() {
        assertThat(baja.ejecutar("MDT-0001", 500, "RETIRO_SANITARIO").medicamento().getStock()).isZero();
    }

    @Test
    void noDejaElStockNegativo() {
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 501, "DETERIORO"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("solo hay 500")
                .extracting("codigoError").isEqualTo("STOCK_INSUFICIENTE");
        verify(medicamentos, never()).actualizarStock(any(), anyInt());
    }

    @Test
    void validaCantidadYMotivo() {
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", null, "VENCIMIENTO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 0, "VENCIMIENTO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 10001, "VENCIMIENTO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 5, null)).hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 5, " ")).hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> baja.ejecutar("MDT-0001", 5, "ROBO")).hasMessageContaining("no valido");
    }

    @Test
    void medicamentoInexistenteEs404() {
        when(medicamentos.buscarPorCodigo("MDT-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baja.ejecutar("MDT-9999", 5, "DETERIORO")).isInstanceOf(NoEncontradoException.class);
    }

    // ---------- Disponibilidad ----------

    @Test
    void indicaQueFaltaYCuanto() {
        when(formulas.buscarPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(Datos.formula()));
        Formula f = Datos.formula();
        f.getItems().set(1, new com.mediagenda.farmacia.domain.model.ItemFormula(2L, Datos.ibuprofeno(), 5, null));
        when(formulas.buscarPorCodigo("FOR-2026-000002")).thenReturn(Optional.of(f));
        VerificarDisponibilidadUseCase uc = new VerificarDisponibilidadUseCase(formulas);

        DisponibilidadFormula alcanza = uc.ejecutar("FOR-2026-000001");
        DisponibilidadFormula falta = uc.ejecutar("FOR-2026-000002");

        assertThat(alcanza.dispensable()).isTrue();
        assertThat(alcanza.items()).extracting(DisponibilidadFormula.Item::faltan).containsExactly(0, 0);
        assertThat(falta.dispensable()).isFalse();
        assertThat(falta.items().get(1)).isEqualTo(new DisponibilidadFormula.Item("MDT-0002", "Ibuprofeno", 5, 3, 2));
    }

    @Test
    void soloRevisaFormulasEmitidas() {
        Formula dispensada = Datos.formula();
        dispensada.setEstado(EstadoFormula.DISPENSADA);
        when(formulas.buscarPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(dispensada));
        when(formulas.buscarPorCodigo("FOR-2026-999999")).thenReturn(Optional.empty());
        VerificarDisponibilidadUseCase uc = new VerificarDisponibilidadUseCase(formulas);

        assertThatThrownBy(() -> uc.ejecutar("FOR-2026-000001"))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigoError").isEqualTo("FORMULA_NO_EMITIDA");
        assertThatThrownBy(() -> uc.ejecutar("FOR-2026-999999")).isInstanceOf(NoEncontradoException.class);
    }
}
