package com.mediagenda.farmacia.infrastructure.entry_points;

import com.mediagenda.farmacia.domain.model.BajaInventario;
import com.mediagenda.farmacia.domain.model.DisponibilidadFormula;
import com.mediagenda.farmacia.domain.usecase.DarDeBajaMedicamentoUseCase;
import com.mediagenda.farmacia.domain.usecase.VerificarDisponibilidadUseCase;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.BajaPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.BajaRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.DisponibilidadRespuesta;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de inventario y de preparacion de entregas, distintas del CRUD. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OperacionesFarmaciaController {

    private final DarDeBajaMedicamentoUseCase darDeBaja;
    private final VerificarDisponibilidadUseCase verificarDisponibilidad;

    @PostMapping("/medicamentos/{codigo}/bajas")
    public ResponseEntity<BajaRespuesta> darDeBaja(@PathVariable String codigo, @RequestBody BajaPeticion peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        BajaInventario baja = darDeBaja.ejecutar(codigo, peticion.cantidad(), peticion.motivo());
        return ResponseEntity.ok(new BajaRespuesta(baja.medicamento().getCodigo(), baja.medicamento().getNombre(),
                baja.medicamento().getPresentacion(), baja.cantidad(), baja.motivo().name(), baja.medicamento().getStock()));
    }

    @GetMapping("/formulas/{codigo}/disponibilidad")
    public ResponseEntity<DisponibilidadRespuesta> disponibilidad(@PathVariable String codigo) {
        DisponibilidadFormula d = verificarDisponibilidad.ejecutar(codigo);
        return ResponseEntity.ok(new DisponibilidadRespuesta(d.codigoFormula(), d.dispensable(), d.items().stream()
                .map(i -> new DisponibilidadRespuesta.Item(i.codigoMedicamento(), i.medicamento(), i.pide(), i.hay(),
                        i.faltan()))
                .toList()));
    }
}
