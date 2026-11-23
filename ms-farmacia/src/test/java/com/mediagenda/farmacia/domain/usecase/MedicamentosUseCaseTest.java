package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicamentosUseCaseTest {

    private MedicamentoGateway medicamentos;

    @BeforeEach
    void preparar() {
        medicamentos = mock(MedicamentoGateway.class);
        when(medicamentos.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(medicamentos.buscarPorCodigo("MDT-0001")).thenReturn(Optional.of(Datos.acetaminofen()));
        when(medicamentos.buscarPorCodigo("MDT-9999")).thenReturn(Optional.empty());
    }

    @Test
    void registra() {
        when(medicamentos.siguienteConsecutivo()).thenReturn(9L);
        Medicamento nuevo = new RegistrarMedicamentoUseCase(medicamentos).ejecutar(
                Medicamento.builder().nombre(" Naproxeno ").presentacion(" Tabletas 250 mg ").stock(40).build());

        assertThat(nuevo.getCodigo()).isEqualTo("MDT-0009");
        assertThat(nuevo.getNombre()).isEqualTo("Naproxeno");
        assertThat(nuevo.getPresentacion()).isEqualTo("Tabletas 250 mg");
        assertThat(nuevo.isActivo()).isTrue();
        assertThat(nuevo.getIdMedicamento()).isNotNull();
    }

    @Test
    void registrarValida() {
        RegistrarMedicamentoUseCase useCase = new RegistrarMedicamentoUseCase(medicamentos);
        Medicamento base = Medicamento.builder().nombre("Naproxeno").presentacion("Tabletas 250 mg").stock(1).build();
        when(medicamentos.existe("Acetaminofen", "Tabletas 500 mg")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().nombre("N").build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().presentacion(null).build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("presentacion");
        assertThatThrownBy(() -> useCase.ejecutar(base.toBuilder().stock(-1).build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("stock");
        assertThatThrownBy(() -> useCase.ejecutar(Datos.acetaminofen()))
                .isInstanceOf(ConflictoException.class).extracting("codigoError").isEqualTo("MEDICAMENTO_DUPLICADO");
    }

    @Test
    void consultaYLista() {
        Pagina<Medicamento> pagina = new Pagina<>(List.of(Datos.acetaminofen()), 0, 20, 1, 1);
        when(medicamentos.listar(null, 0, 20)).thenReturn(pagina);
        when(medicamentos.listar("acet", 0, 20)).thenReturn(pagina);

        assertThat(new ConsultarMedicamentoUseCase(medicamentos).ejecutar("MDT-0001").getNombre()).isEqualTo("Acetaminofen");
        assertThatThrownBy(() -> new ConsultarMedicamentoUseCase(medicamentos).ejecutar("MDT-9999"))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(new ListarMedicamentosUseCase(medicamentos).ejecutar(" ", 0, 20)).isSameAs(pagina);
        assertThat(new ListarMedicamentosUseCase(medicamentos).ejecutar(null, 0, 20)).isSameAs(pagina);
        assertThat(new ListarMedicamentosUseCase(medicamentos).ejecutar(" acet ", 0, 20)).isSameAs(pagina);
    }

    @Test
    void reabastece() {
        Medicamento resultado = new ReabastecerMedicamentoUseCase(medicamentos).ejecutar("MDT-0001", 100);

        assertThat(resultado.getStock()).isEqualTo(600);
        verify(medicamentos).sumarStock(Datos.ID_ACETAMINOFEN, 100);
    }

    @Test
    void reabastecerValida() {
        ReabastecerMedicamentoUseCase useCase = new ReabastecerMedicamentoUseCase(medicamentos);

        assertThatThrownBy(() -> useCase.ejecutar("MDT-0001", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MDT-0001", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("MDT-9999", 5)).isInstanceOf(NoEncontradoException.class);
        verify(medicamentos, never()).sumarStock(any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
