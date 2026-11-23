package com.mediagenda.citas.infrastructure.tareas;

import com.mediagenda.citas.domain.usecase.EnviarRecordatoriosUseCase;
import com.mediagenda.citas.domain.usecase.ProcesarTriageUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TareasTest {

    @Test
    void solicitudDeTriageEjecutaElCasoDeUsoYNoPropagaErrores() {
        ProcesarTriageUseCase procesar = mock(ProcesarTriageUseCase.class);
        SolicitudTriageAsincrona solicitud = new SolicitudTriageAsincrona(procesar);

        solicitud.solicitar("CIT-2026-000001");
        doThrow(new IllegalStateException("falla")).when(procesar).ejecutar("CIT-2026-000002");

        assertThatCode(() -> solicitud.solicitar("CIT-2026-000002")).doesNotThrowAnyException();
        verify(procesar).ejecutar("CIT-2026-000001");
    }

    @Test
    void recordatoriosProgramados() {
        EnviarRecordatoriosUseCase enviar = mock(EnviarRecordatoriosUseCase.class);
        when(enviar.ejecutar()).thenReturn(2).thenReturn(0).thenThrow(new IllegalStateException("base caida"));
        RecordatoriosProgramados tarea = new RecordatoriosProgramados(enviar);

        tarea.ejecutar();
        tarea.ejecutar();
        assertThatCode(tarea::ejecutar).doesNotThrowAnyException();
        verify(enviar, times(3)).ejecutar();
    }
}
