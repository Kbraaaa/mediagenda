package com.mediagenda.farmacia.infrastructure.entry_points;

import com.mediagenda.farmacia.domain.model.FiltroFormulas;
import com.mediagenda.farmacia.domain.usecase.AnularFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ConsultarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.DispensarFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.EmitirFormulaUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarFormulasUseCase;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.EmitirFormulaPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.FormulaRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.farmacia.infrastructure.mapper.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Las 5 APIs de ms-farmacia. */
@RestController
@RequestMapping("/api/v1/formulas")
@RequiredArgsConstructor
public class FormulaController {

    private final EmitirFormulaUseCase emitirFormula;
    private final ConsultarFormulaUseCase consultarFormula;
    private final ListarFormulasUseCase listarFormulas;
    private final DispensarFormulaUseCase dispensarFormula;
    private final AnularFormulaUseCase anularFormula;
    private final DtoMapper mapper;

    /** 1. Emitir formula para una cita ATENDIDA: 201, 400, 404, 409, 503. */
    @PostMapping
    public ResponseEntity<FormulaRespuesta> emitir(@RequestBody EmitirFormulaPeticion peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        var formula = emitirFormula.ejecutar(peticion.codigoCita(), mapper.toDomain(peticion.items()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRespuesta(formula));
    }

    /** 2. Consultar: 200, 404. */
    @GetMapping("/{codigo}")
    public ResponseEntity<FormulaRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultarFormula.ejecutar(codigo)));
    }

    /** 3. Listar por paciente y estado, paginado: 200, 400. */
    @GetMapping
    public ResponseEntity<PaginaRespuesta<FormulaRespuesta>> listar(
            @RequestParam(required = false) String paciente,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        FiltroFormulas filtro = new FiltroFormulas(DtoMapper.textoOpcional(paciente), DtoMapper.estadoOpcional(estado));
        return ResponseEntity.ok(mapper.toRespuesta(listarFormulas.ejecutar(filtro, page, size), mapper::toRespuesta));
    }

    /** 4. Dispensar (entregar medicamentos y descontar stock): 200, 404, 409. */
    @PostMapping("/{codigo}/dispensacion")
    public ResponseEntity<FormulaRespuesta> dispensar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(dispensarFormula.ejecutar(codigo)));
    }

    /** 5. Anular una formula no entregada: 200, 404, 409. */
    @PatchMapping("/{codigo}/anulacion")
    public ResponseEntity<FormulaRespuesta> anular(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(anularFormula.ejecutar(codigo)));
    }
}
