package com.mediagenda.pacientes.infrastructure.entry_points;

import com.mediagenda.pacientes.domain.model.CompatibilidadSanguinea;
import com.mediagenda.pacientes.domain.model.VerificacionAlergia;
import com.mediagenda.pacientes.domain.usecase.CambiarDocumentoUseCase;
import com.mediagenda.pacientes.domain.usecase.CompatibilidadSanguineaUseCase;
import com.mediagenda.pacientes.domain.usecase.ReactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarAlergiaUseCase;
import com.mediagenda.pacientes.domain.usecase.VerificarAlergiaUseCase;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.AlergiaPeticion;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.CambioDocumentoPeticion;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.CompatibilidadRespuesta;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacienteRespuesta;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.VerificacionAlergiaRespuesta;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de negocio sobre un paciente, distintas del CRUD (que esta en PacienteController). */
@RestController
@RequestMapping("/api/v1/pacientes/{codigo}")
@RequiredArgsConstructor
public class OperacionesPacienteController {

    private final ReactivarPacienteUseCase reactivar;
    private final RegistrarAlergiaUseCase registrarAlergia;
    private final VerificarAlergiaUseCase verificarAlergia;
    private final CambiarDocumentoUseCase cambiarDocumento;
    private final CompatibilidadSanguineaUseCase compatibilidad;
    private final PacienteDtoMapper mapper;

    @PatchMapping("/reactivacion")
    public ResponseEntity<PacienteRespuesta> reactivar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(reactivar.ejecutar(codigo)));
    }

    @PostMapping("/alergias")
    public ResponseEntity<PacienteRespuesta> registrarAlergia(@PathVariable String codigo,
                                                              @RequestBody AlergiaPeticion peticion) {
        String alergia = peticion == null ? null : peticion.alergia();
        return ResponseEntity.ok(mapper.toRespuesta(registrarAlergia.ejecutar(codigo, alergia)));
    }

    @GetMapping("/alergias/verificacion")
    public ResponseEntity<VerificacionAlergiaRespuesta> verificarAlergia(@PathVariable String codigo,
                                                                         @RequestParam(required = false) String sustancia) {
        VerificacionAlergia v = verificarAlergia.ejecutar(codigo, sustancia);
        return ResponseEntity.ok(new VerificacionAlergiaRespuesta(v.codigoPaciente(), v.sustancia(), v.alerta(),
                v.coincidencias()));
    }

    @PostMapping("/cambio-documento")
    public ResponseEntity<PacienteRespuesta> cambiarDocumento(@PathVariable String codigo,
                                                              @RequestBody CambioDocumentoPeticion peticion) {
        String numero = peticion == null ? null : peticion.numeroDocumento();
        return ResponseEntity.ok(mapper.toRespuesta(cambiarDocumento.ejecutar(codigo, numero)));
    }

    @GetMapping("/compatibilidad-sanguinea")
    public ResponseEntity<CompatibilidadRespuesta> compatibilidad(@PathVariable String codigo,
                                                                  @RequestParam(required = false) String donante) {
        CompatibilidadSanguinea c = compatibilidad.ejecutar(codigo, donante);
        return ResponseEntity.ok(new CompatibilidadRespuesta(c.codigoReceptor(), c.tipoReceptor(),
                c.codigoDonante(), c.tipoDonante(), c.compatible(), c.explicacion()));
    }
}
