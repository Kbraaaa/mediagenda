package com.mediagenda.pacientes.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloDominioTest {

    @Test
    void tipoDocumentoAceptaMayusculasMinusculasYEspacios() {
        assertThat(TipoDocumento.desde("cc")).isEqualTo(TipoDocumento.CC);
        assertThat(TipoDocumento.desde(" PA ")).isEqualTo(TipoDocumento.PA);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void tipoDocumentoObligatorio(String valor) {
        assertThatThrownBy(() -> TipoDocumento.desde(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void tipoDocumentoInvalido() {
        assertThatThrownBy(() -> TipoDocumento.desde("NIT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CC, TI, CE o PA");
    }

    @Test
    void codigoPacienteTieneSeisDigitos() {
        assertThat(CodigoPaciente.formatear(1)).isEqualTo("PAC-000001");
        assertThat(CodigoPaciente.formatear(123456)).isEqualTo("PAC-123456");
    }

    @Test
    void codigoPacienteRechazaConsecutivoNoPositivo() {
        assertThatThrownBy(() -> CodigoPaciente.formatear(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paginaValidaParametros() {
        Pagina.validarParametros(0, 1);
        Pagina.validarParametros(3, Pagina.TAMANO_MAXIMO);

        assertThatThrownBy(() -> Pagina.validarParametros(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
        assertThatThrownBy(() -> Pagina.validarParametros(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagina.validarParametros(0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paginaConservaSusDatos() {
        Pagina<String> pagina = new Pagina<>(List.of("a", "b"), 1, 2, 5, 3);
        assertThat(pagina.contenido()).containsExactly("a", "b");
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }
}
