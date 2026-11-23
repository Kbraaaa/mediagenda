package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.domain.usecase.AbrirAgendaUseCase;
import com.mediagenda.citas.domain.usecase.ConsultarMedicoUseCase;
import com.mediagenda.citas.domain.usecase.ListarMedicosUseCase;
import com.mediagenda.citas.domain.usecase.RegistrarMedicoUseCase;
import com.mediagenda.citas.infrastructure.entry_points.dto.AbrirAgendaPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.FranjaRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.MedicoPeticion;
import com.mediagenda.citas.infrastructure.entry_points.dto.MedicoRespuesta;
import com.mediagenda.citas.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.citas.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Apoyo de agenda: registrar medicos y abrir sus franjas. */
@RestController
@RequestMapping("/api/v1/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final RegistrarMedicoUseCase registrarMedico;
    private final ConsultarMedicoUseCase consultarMedico;
    private final ListarMedicosUseCase listarMedicos;
    private final AbrirAgendaUseCase abrirAgenda;
    private final DtoMapper mapper;

    @PostMapping
    public ResponseEntity<MedicoRespuesta> registrar(@RequestBody MedicoPeticion peticion) {
        var medico = registrarMedico.ejecutar(mapper.toDomain(peticion));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRespuesta(medico));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<MedicoRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultarMedico.ejecutar(codigo)));
    }

    @GetMapping
    public ResponseEntity<PaginaRespuesta<MedicoRespuesta>> listar(
            @RequestParam(required = false) String especialidad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pagina = listarMedicos.ejecutar(DtoMapper.especialidadOpcional(especialidad), page, size);
        return ResponseEntity.ok(mapper.toRespuesta(pagina, mapper::toRespuesta));
    }

    @PostMapping("/{codigo}/franjas")
    public ResponseEntity<List<FranjaRespuesta>> abrirAgenda(@PathVariable String codigo,
                                                             @RequestBody AbrirAgendaPeticion peticion) {
        var franjas = abrirAgenda.ejecutar(codigo, mapper.toDomain(peticion));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(franjas.stream().map(mapper::toRespuesta).toList());
    }
}
