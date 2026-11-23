package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.domain.model.FiltroCitas;
import com.mediagenda.citas.domain.usecase.AgendarCitaUseCase;
import com.mediagenda.citas.domain.usecase.AtenderCitaUseCase;
import com.mediagenda.citas.domain.usecase.CancelarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarCitaUseCase;
import com.mediagenda.citas.domain.usecase.ListarCitasUseCase;
import com.mediagenda.citas.domain.usecase.ReprogramarCitaUseCase;
import com.mediagenda.citas.infrastructure.entry_points.dto.AgendarCitaPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.CitaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.ReprogramarCitaPeticion;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Las 5 APIs principales de ms-citas, mas la de atencion que usa ms-farmacia. */
@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
public class CitaController {

    private final AgendarCitaUseCase agendarCita;
    private final ConsultarCitaUseCase consultarCita;
    private final ListarCitasUseCase listarCitas;
    private final ReprogramarCitaUseCase reprogramarCita;
    private final CancelarCitaUseCase cancelarCita;
    private final AtenderCitaUseCase atenderCita;
    private final DtoMapper mapper;

    /** 1. Agendar: 201, 400, 404, 409 franja ocupada, 422 festivo, 503 ms-pacientes caido. */
    @PostMapping
    public ResponseEntity<CitaRespuesta> agendar(@RequestBody AgendarCitaPeticion peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        var cita = agendarCita.ejecutar(peticion.codigoPaciente(), peticion.idFranja(), peticion.sintomas());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRespuesta(cita));
    }

    /** 2. Consultar (incluye el triage): 200, 404. */
    @GetMapping("/{codigo}")
    public ResponseEntity<CitaRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultarCita.ejecutar(codigo)));
    }

    /** 3. Listar con filtros y paginacion: 200, 400. */
    @GetMapping
    public ResponseEntity<PaginaRespuesta<CitaRespuesta>> listar(
            @RequestParam(required = false) String paciente,
            @RequestParam(required = false) String medico,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        FiltroCitas filtro = new FiltroCitas(DtoMapper.textoOpcional(paciente), DtoMapper.textoOpcional(medico),
                DtoMapper.estadoOpcional(estado));
        return ResponseEntity.ok(mapper.toRespuesta(listarCitas.ejecutar(filtro, page, size), mapper::toRespuesta));
    }

    /** 4. Reprogramar a otra franja: 200, 400, 404, 409, 422. */
    @PutMapping("/{codigo}/reprogramacion")
    public ResponseEntity<CitaRespuesta> reprogramar(@PathVariable String codigo,
                                                     @RequestBody ReprogramarCitaPeticion peticion) {
        Long idFranja = peticion == null ? null : peticion.idFranja();
        return ResponseEntity.ok(mapper.toRespuesta(reprogramarCita.ejecutar(codigo, idFranja)));
    }

    /** 5. Cancelar y liberar la franja: 200, 404, 409. */
    @PatchMapping("/{codigo}/cancelacion")
    public ResponseEntity<CitaRespuesta> cancelar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(cancelarCita.ejecutar(codigo)));
    }

    /** Apoyo: el medico marca la cita como atendida (requisito para formular en ms-farmacia). */
    @PatchMapping("/{codigo}/atencion")
    public ResponseEntity<CitaRespuesta> atender(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(atenderCita.ejecutar(codigo)));
    }
}
