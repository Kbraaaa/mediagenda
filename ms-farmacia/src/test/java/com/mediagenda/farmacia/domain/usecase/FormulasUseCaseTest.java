package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.DatosCita;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.EventoFormula;
import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.SolicitudItem;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import com.mediagenda.farmacia.domain.model.gateway.TransaccionGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormulasUseCaseTest {

    private CitaGateway citas;
    private MedicamentoGateway medicamentos;
    private FormulaGateway formulas;
    private EventoGateway eventos;
    private TransaccionGateway transaccion;
    private EmitirFormulaUseCase emitir;

    private static final List<SolicitudItem> ITEMS = List.of(
            new SolicitudItem("MDT-0001", 20, " 1 cada 8 horas "),
            new SolicitudItem("MDT-0002", 2, "  "));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void preparar() {
        citas = mock(CitaGateway.class);
        medicamentos = mock(MedicamentoGateway.class);
        formulas = mock(FormulaGateway.class);
        eventos = mock(EventoGateway.class);
        transaccion = mock(TransaccionGateway.class);
        when(transaccion.ejecutar(any())).thenAnswer(i -> ((Supplier<Object>) i.getArgument(0)).get());
        emitir = new EmitirFormulaUseCase(citas, medicamentos, formulas, eventos, Datos.RELOJ);
        when(citas.buscar("CIT-2026-000001")).thenReturn(Optional.of(Datos.citaAtendida()));
        when(medicamentos.buscarPorCodigo("MDT-0001")).thenReturn(Optional.of(Datos.acetaminofen()));
        when(medicamentos.buscarPorCodigo("MDT-0002")).thenReturn(Optional.of(Datos.ibuprofeno()));
        when(formulas.siguienteConsecutivo()).thenReturn(12L);
        when(formulas.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- Emitir ----------

    @Test
    void emiteFormulaYPublicaEvento() {
        Formula formula = emitir.ejecutar(" CIT-2026-000001 ", ITEMS);

        assertThat(formula.getCodigo()).isEqualTo("FOR-2026-000012");
        assertThat(formula.getEstado()).isEqualTo(EstadoFormula.EMITIDA);
        assertThat(formula.getCorreoPaciente()).isEqualTo("laura@correo.com");
        assertThat(formula.getItems()).hasSize(2);
        assertThat(formula.getItems().get(0).indicaciones()).isEqualTo("1 cada 8 horas");
        assertThat(formula.getItems().get(1).indicaciones()).isNull();
        ArgumentCaptor<EventoFormula> evento = ArgumentCaptor.forClass(EventoFormula.class);
        verify(eventos).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(EventoFormula.EMITIDA);
    }

    @Test
    void emitirValidaLaSolicitud() {
        List<SolicitudItem> once = Collections.nCopies(11, new SolicitudItem("MDT-0001", 1, null));
        List<SolicitudItem> conNulo = new ArrayList<>(Arrays.asList(new SolicitudItem("MDT-0001", 1, null), null));

        assertThatThrownBy(() -> emitir.ejecutar(null, ITEMS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", once))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("10");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", conNulo))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vacio");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of(new SolicitudItem(" ", 1, null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of(new SolicitudItem("MDT-0001", 0, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cantidad");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of(new SolicitudItem("MDT-0001", 101, null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001",
                List.of(new SolicitudItem("MDT-0001", 1, "x".repeat(301)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("300");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001",
                List.of(new SolicitudItem("MDT-0001", 1, null), new SolicitudItem("mdt-0001", 2, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("repetido");
        verify(citas, never()).buscar(any());
    }

    @Test
    void emitirReglasDeNegocio() {
        when(citas.buscar("CIT-404")).thenReturn(Optional.empty());
        when(citas.buscar("CIT-AGENDADA")).thenReturn(Optional.of(
                new DatosCita("CIT-AGENDADA", "PAC-000001", "Laura", "l@c.co", null, "AGENDADA")));

        assertThatThrownBy(() -> emitir.ejecutar("CIT-404", ITEMS))
                .isInstanceOf(NoEncontradoException.class).extracting("codigoError").isEqualTo("CITA_NO_ENCONTRADA");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-AGENDADA", ITEMS))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("CITA_NO_ATENDIDA");

        when(formulas.existeActivaParaCita("CIT-2026-000001")).thenReturn(true);
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", ITEMS))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("FORMULA_EXISTENTE");
        verify(formulas, never()).guardar(any());
    }

    @Test
    void emitirConMedicamentoInexistenteOInactivo() {
        when(medicamentos.buscarPorCodigo("MDT-9999")).thenReturn(Optional.empty());
        Medicamento inactivo = Datos.ibuprofeno().toBuilder().codigo("MDT-0003").activo(false).build();
        when(medicamentos.buscarPorCodigo("MDT-0003")).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of(new SolicitudItem("MDT-9999", 1, null))))
                .isInstanceOf(NoEncontradoException.class).extracting("codigoError").isEqualTo("MEDICAMENTO_NO_ENCONTRADO");
        assertThatThrownBy(() -> emitir.ejecutar("CIT-2026-000001", List.of(new SolicitudItem("MDT-0003", 1, null))))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("MEDICAMENTO_INACTIVO");
    }

    // ---------- Dispensar ----------

    private DispensarFormulaUseCase dispensar() {
        return new DispensarFormulaUseCase(formulas, medicamentos, transaccion, eventos, Datos.RELOJ);
    }

    @Test
    void dispensaDescontandoStockEnUnaTransaccion() {
        when(formulas.bloquearPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(Datos.formula()));
        when(medicamentos.bloquear(List.of(Datos.ID_ACETAMINOFEN, Datos.ID_IBUPROFENO)))
                .thenReturn(List.of(Datos.acetaminofen(), Datos.ibuprofeno()));

        Formula formula = dispensar().ejecutar("FOR-2026-000001");

        assertThat(formula.getEstado()).isEqualTo(EstadoFormula.DISPENSADA);
        assertThat(formula.getFechaDispensacion()).isNotNull();
        verify(medicamentos).actualizarStock(Datos.ID_ACETAMINOFEN, 480);
        verify(medicamentos).actualizarStock(Datos.ID_IBUPROFENO, 1);
        verify(formulas).actualizarEstado(eq("FOR-2026-000001"), eq(EstadoFormula.DISPENSADA), any());
        verify(eventos).publicar(any());
    }

    @Test
    void sinStockSuficienteNoSeTocaNada() {
        Formula formula = Datos.formula();
        formula.getItems().set(1, new com.mediagenda.farmacia.domain.model.ItemFormula(2L, Datos.ibuprofeno(), 5, null));
        when(formulas.bloquearPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(formula));
        when(medicamentos.bloquear(any())).thenReturn(List.of(Datos.acetaminofen(), Datos.ibuprofeno()));

        assertThatThrownBy(() -> dispensar().ejecutar("FOR-2026-000001"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ibuprofeno (pide 5, hay 3)")
                .extracting("codigoError").isEqualTo("STOCK_INSUFICIENTE");
        verify(medicamentos, never()).actualizarStock(any(), anyInt());
        verify(formulas, never()).actualizarEstado(any(), any(), any());
        verify(eventos, never()).publicar(any());
    }

    @Test
    void noSeDispensaDosVeces() {
        Formula yaEntregada = Datos.formula();
        yaEntregada.setEstado(EstadoFormula.DISPENSADA);
        when(formulas.bloquearPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(yaEntregada));
        when(formulas.bloquearPorCodigo("FOR-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispensar().ejecutar("FOR-2026-000001"))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("FORMULA_NO_EMITIDA");
        assertThatThrownBy(() -> dispensar().ejecutar("FOR-404"))
                .isInstanceOf(NoEncontradoException.class).extracting("codigoError").isEqualTo("FORMULA_NO_ENCONTRADA");
    }

    // ---------- Consultar, listar, anular ----------

    @Test
    void consultaListaYAnula() {
        when(formulas.buscarPorCodigo("FOR-2026-000001")).thenReturn(Optional.of(Datos.formula()));
        when(formulas.buscarPorCodigo("FOR-404")).thenReturn(Optional.empty());
        FiltroFormulas filtro = new FiltroFormulas("PAC-000001", null);
        Pagina<Formula> pagina = new Pagina<>(List.of(Datos.formula()), 0, 10, 1, 1);
        when(formulas.listar(filtro, 0, 10)).thenReturn(pagina);

        assertThat(new ConsultarFormulaUseCase(formulas).ejecutar("FOR-2026-000001").getCodigoCita())
                .isEqualTo("CIT-2026-000001");
        assertThatThrownBy(() -> new ConsultarFormulaUseCase(formulas).ejecutar("FOR-404"))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(new ListarFormulasUseCase(formulas).ejecutar(filtro, 0, 10)).isSameAs(pagina);

        Formula anulada = new AnularFormulaUseCase(formulas).ejecutar("FOR-2026-000001");
        assertThat(anulada.getEstado()).isEqualTo(EstadoFormula.ANULADA);
        verify(formulas).actualizarEstado("FOR-2026-000001", EstadoFormula.ANULADA, null);
    }

    @Test
    void validacionesDeTexto() {
        assertThatThrownBy(() -> Validaciones.texto("x".repeat(30), "campo", 1, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 1 y 20");
    }
}
