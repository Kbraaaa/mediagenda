package com.mediagenda.farmacia.infrastructure.entry_points;

import com.mediagenda.farmacia.domain.usecase.ConsultarMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.ListarMedicamentosUseCase;
import com.mediagenda.farmacia.domain.usecase.ReabastecerMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.RegistrarMedicamentoUseCase;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.MedicamentoPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.MedicamentoRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.ReabastecerPeticion;
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

/** Apoyo: inventario de medicamentos. */
@RestController
@RequestMapping("/api/v1/medicamentos")
@RequiredArgsConstructor
public class MedicamentoController {

    private final RegistrarMedicamentoUseCase registrar;
    private final ConsultarMedicamentoUseCase consultar;
    private final ListarMedicamentosUseCase listar;
    private final ReabastecerMedicamentoUseCase reabastecer;
    private final DtoMapper mapper;

    @PostMapping
    public ResponseEntity<MedicamentoRespuesta> registrar(@RequestBody MedicamentoPeticion peticion) {
        var medicamento = registrar.ejecutar(mapper.toDomain(peticion));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRespuesta(medicamento));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<MedicamentoRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultar.ejecutar(codigo)));
    }

    @GetMapping
    public ResponseEntity<PaginaRespuesta<MedicamentoRespuesta>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mapper.toRespuesta(listar.ejecutar(nombre, page, size), mapper::toRespuesta));
    }

    @PatchMapping("/{codigo}/stock")
    public ResponseEntity<MedicamentoRespuesta> reabastecer(@PathVariable String codigo,
                                                            @RequestBody ReabastecerPeticion peticion) {
        Integer cantidad = peticion == null ? null : peticion.cantidad();
        return ResponseEntity.ok(mapper.toRespuesta(reabastecer.ejecutar(codigo, cantidad)));
    }
}
