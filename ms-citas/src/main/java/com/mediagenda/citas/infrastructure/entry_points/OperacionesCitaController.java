package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.domain.usecase.ReintentarTriageUseCase;
import com.mediagenda.citas.infrastructure.entry_points.dto.CitaRespuesta;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de negocio sobre una cita que no estan en CitaController. */
@RestController
@RequestMapping("/api/v1/citas/{codigo}")
@RequiredArgsConstructor
public class OperacionesCitaController {

    private final ReintentarTriageUseCase reintentarTriage;
    private final DtoMapper mapper;

    /** 202: la solicitud se acepto y el analisis corre en segundo plano. */
    @PostMapping("/triage")
    public ResponseEntity<CitaRespuesta> reintentarTriage(@PathVariable String codigo) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRespuesta(reintentarTriage.ejecutar(codigo)));
    }
}
