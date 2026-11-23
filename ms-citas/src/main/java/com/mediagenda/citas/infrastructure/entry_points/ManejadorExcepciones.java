package com.mediagenda.citas.infrastructure.entry_points;

import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.exception.DiaFestivoException;
import com.mediagenda.citas.domain.model.exception.NoEncontradoException;
import com.mediagenda.citas.domain.model.exception.ServicioNoDisponibleException;
import com.mediagenda.citas.infrastructure.entry_points.dto.ErrorRespuesta;
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
 * 400 = lo enviado esta mal; 404 = no existe; 409 = valido pero el estado no lo permite;
 * 422 = regla externa (festivo); 503 = un servicio del que dependemos no responde.
 */
@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getCodigoError(), e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getCodigoError(), e.getMessage());
    }

    /** Segunda barrera: el indice unico de la base detecto una doble reserva o un duplicado. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "CONFLICTO_DATOS",
                "La operacion choca con otro registro (por ejemplo, la franja ya fue reservada)");
    }

    @ExceptionHandler(DiaFestivoException.class)
    public ResponseEntity<ErrorRespuesta> festivo(DiaFestivoException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "DIA_FESTIVO", e.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> noDisponible(ServicioNoDisponibleException e) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", e.getMessage());
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

    /** RuntimeException (y no Exception) para que Spring siga respondiendo 405, 404 de ruta, etc. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorRespuesta> inesperado(RuntimeException e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrio un error inesperado");
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(codigo, mensaje));
    }
}
