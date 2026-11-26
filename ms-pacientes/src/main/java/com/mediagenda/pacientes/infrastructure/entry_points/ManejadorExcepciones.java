package com.mediagenda.pacientes.infrastructure.entry_points;

import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.ReglaNegocioException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.infrastructure.entry_points.dto.ErrorRespuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte las excepciones en codigos HTTP con el formato {"codigo", "mensaje"}.
 * 400 = lo enviado esta mal; 404 = no existe; 409 = lo enviado esta bien pero el estado no lo permite.
 */
@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(PacienteNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(PacienteNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "PACIENTE_NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(PacienteDuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> duplicado(PacienteDuplicadoException e) {
        return responder(HttpStatus.CONFLICT, "PACIENTE_DUPLICADO", e.getMessage());
    }

    @ExceptionHandler(PacienteInactivoException.class)
    public ResponseEntity<ErrorRespuesta> inactivo(PacienteInactivoException e) {
        return responder(HttpStatus.CONFLICT, "PACIENTE_INACTIVO", e.getMessage());
    }

    /** Dos registros simultaneos con el mismo documento: la restriccion UNIQUE de la base decide. */
    @ExceptionHandler(ConflictoPacienteException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoPacienteException e) {
        return responder(HttpStatus.CONFLICT, e.getCodigoError(), e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getCodigoError(), e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "PACIENTE_DUPLICADO",
                "Ya existe un paciente con ese documento o correo");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuesta> peticionInvalida(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, "PETICION_INVALIDA", e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> formatoInvalido(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "PETICION_INVALIDA",
                "El cuerpo o los parametros de la peticion no tienen un formato valido");
    }

    /**
     * Cualquier otro error de la aplicacion. Se usa RuntimeException (y no Exception) para que
     * Spring siga respondiendo 405, 404 de ruta inexistente, etc. con sus codigos correctos.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorRespuesta> inesperado(RuntimeException e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrio un error inesperado");
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(codigo, mensaje));
    }
}
