package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.PacientesDePrueba;
import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrarPacienteUseCaseTest {

    private PacienteGateway gateway;
    private RegistrarPacienteUseCase useCase;

    @BeforeEach
    void preparar() {
        gateway = mock(PacienteGateway.class);
        useCase = new RegistrarPacienteUseCase(gateway);
        when(gateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registraConCodigoActivoYDatosNormalizados() {
        when(gateway.siguienteConsecutivo()).thenReturn(42L);
        Paciente datos = PacientesDePrueba.datosValidos().toBuilder()
                .nombres("  Laura  ")
                .correo("  LAURA.Gomez@Correo.com ")
                .telefono("   ")
                .alergias("")
                .build();

        Paciente registrado = useCase.ejecutar(datos);

        assertThat(registrado.getCodigo()).isEqualTo("PAC-000042");
        assertThat(registrado.getIdPaciente()).isNotNull();
        assertThat(registrado.isActivo()).isTrue();
        assertThat(registrado.getFechaRegistro()).isNotNull();
        assertThat(registrado.getNombres()).isEqualTo("Laura");
        assertThat(registrado.getCorreo()).isEqualTo("laura.gomez@correo.com");
        assertThat(registrado.getTelefono()).isNull();
        assertThat(registrado.getAlergias()).isNull();
    }

    @Test
    void buscaDuplicadosConElCorreoNormalizado() {
        when(gateway.siguienteConsecutivo()).thenReturn(1L);
        useCase.ejecutar(PacientesDePrueba.datosValidos().toBuilder().correo("LAURA@CORREO.COM").build());

        ArgumentCaptor<String> correo = ArgumentCaptor.forClass(String.class);
        verify(gateway).existeCorreo(correo.capture());
        assertThat(correo.getValue()).isEqualTo("laura@correo.com");
    }

    @Test
    void rechazaDocumentoDuplicado() {
        when(gateway.existeDocumento(TipoDocumento.CC, "1069123456")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(PacientesDePrueba.datosValidos()))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessageContaining("documento CC 1069123456");
        verify(gateway, never()).guardar(any());
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(gateway.existeCorreo("laura.gomez@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(PacientesDePrueba.datosValidos()))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessageContaining("correo");
        verify(gateway, never()).guardar(any());
    }

    @Test
    void noConsultaLaBaseSiLosDatosSonInvalidos() {
        Paciente invalido = PacientesDePrueba.datosValidos().toBuilder().nombres("").build();

        assertThatThrownBy(() -> useCase.ejecutar(invalido)).isInstanceOf(IllegalArgumentException.class);
        verify(gateway, never()).existeDocumento(any(), any());
    }
}
