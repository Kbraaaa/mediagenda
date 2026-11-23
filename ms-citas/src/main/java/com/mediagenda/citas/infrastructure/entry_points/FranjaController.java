package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.domain.model.FiltroFranjas;
import com.mediagenda.citas.domain.usecase.ListarFranjasUseCase;
import com.mediagenda.citas.infrastructure.entry_points.dto.FranjaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Consulta de franjas: por defecto solo las disponibles. */
@RestController
@RequestMapping("/api/v1/franjas")
@RequiredArgsConstructor
public class FranjaController {

    private final ListarFranjasUseCase listarFranjas;
    private final DtoMapper mapper;

    @GetMapping
    public ResponseEntity<PaginaRespuesta<FranjaRespuesta>> listar(
            @RequestParam(required = false) String medico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "true") boolean disponible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FiltroFranjas filtro = new FiltroFranjas(DtoMapper.textoOpcional(medico), fecha, disponible);
        return ResponseEntity.ok(mapper.toRespuesta(listarFranjas.ejecutar(filtro, page, size), mapper::toRespuesta));
    }
}
