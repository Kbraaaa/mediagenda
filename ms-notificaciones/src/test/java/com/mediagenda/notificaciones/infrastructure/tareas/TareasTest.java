package com.mediagenda.notificaciones.infrastructure.tareas;

import com.mediagenda.notificaciones.domain.usecase.EnviarPendienteUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarFallidasUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TareasTest {

    @Test
    void envioAsincronoNoPropagaErrores() {
        EnviarPendienteUseCase enviar = mock(EnviarPendienteUseCase.class);
        doThrow(new IllegalStateException("x")).when(enviar).ejecutar("NOT-2");
        SolicitudEnvioAsincrona solicitud = new SolicitudEnvioAsincrona(enviar);

        solicitud.solicitar("NOT-1");
        assertThatCode(() -> solicitud.solicitar("NOT-2")).doesNotThrowAnyException();
        verify(enviar).ejecutar("NOT-1");
    }

    @Test
    void reintentosProgramados() {
        ReintentarFallidasUseCase reintentar = mock(ReintentarFallidasUseCase.class);
        when(reintentar.ejecutar()).thenReturn(1).thenReturn(0).thenThrow(new IllegalStateException("x"));
        ReintentosProgramados tarea = new ReintentosProgramados(reintentar);

        tarea.ejecutar();
        tarea.ejecutar();
        assertThatCode(tarea::ejecutar).doesNotThrowAnyException();
        verify(reintentar, times(3)).ejecutar();
    }
}
