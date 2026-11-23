package com.mediagenda.pacientes.infrastructure.entry_points;

import com.mediagenda.pacientes.domain.usecase.ActualizarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ConsultarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.DesactivarPacienteUseCase;
import com.mediagenda.pacientes.domain.usecase.ListarPacientesUseCase;
import com.mediagenda.pacientes.domain.usecase.RegistrarPacienteUseCase;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacientePeticion;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PacienteRespuesta;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.PaginaRespuesta;
import com.mediagenda.pacientes.infrastructure.mapper.PacienteDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las 5 APIs de ms-pacientes. El controlador solo recibe, llama al caso de uso y responde:
 * las reglas de negocio estan en los casos de uso.
 */
@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final RegistrarPacienteUseCase registrarPaciente;
    private final ConsultarPacienteUseCase consultarPaciente;
    private final ListarPacientesUseCase listarPacientes;
    private final ActualizarPacienteUseCase actualizarPaciente;
    private final DesactivarPacienteUseCase desactivarPaciente;
    private final PacienteDtoMapper mapper;

    /** 1. Registrar paciente: 201, 400, 409. */
    @PostMapping
    public ResponseEntity<PacienteRespuesta> registrar(@RequestBody PacientePeticion peticion) {
        var creado = registrarPaciente.ejecutar(mapper.toDomain(peticion));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRespuesta(creado));
    }

    /** 2. Consultar paciente por codigo: 200, 404. */
    @GetMapping("/{codigo}")
    public ResponseEntity<PacienteRespuesta> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.toRespuesta(consultarPaciente.ejecutar(codigo)));
    }

    /** 3. Listar pacientes activos con filtro por nombre y paginacion: 200, 400. */
    @GetMapping
    public ResponseEntity<PaginaRespuesta<PacienteRespuesta>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mapper.toRespuesta(listarPacientes.ejecutar(nombre, page, size)));
    }

    /** 4. Actualizar datos: 200, 400, 404, 409. */
    @PutMapping("/{codigo}")
    public ResponseEntity<PacienteRespuesta> actualizar(@PathVariable String codigo,
                                                        @RequestBody PacientePeticion peticion) {
        var actualizado = actualizarPaciente.ejecutar(codigo, mapper.toDomain(peticion));
        return ResponseEntity.ok(mapper.toRespuesta(actualizado));
    }

    /** 5. Desactivar (borrado logico): 204, 404. */
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> desactivar(@PathVariable String codigo) {
        desactivarPaciente.ejecutar(codigo);
        return ResponseEntity.noContent().build();
    }
}
