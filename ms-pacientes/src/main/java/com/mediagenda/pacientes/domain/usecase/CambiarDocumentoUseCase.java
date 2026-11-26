package com.mediagenda.pacientes.domain.usecase;

import com.mediagenda.pacientes.domain.model.Paciente;
import com.mediagenda.pacientes.domain.model.TipoDocumento;
import com.mediagenda.pacientes.domain.model.exception.ConflictoPacienteException;
import com.mediagenda.pacientes.domain.model.exception.PacienteDuplicadoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteInactivoException;
import com.mediagenda.pacientes.domain.model.exception.PacienteNoEncontradoException;
import com.mediagenda.pacientes.domain.model.exception.ReglaNegocioException;
import com.mediagenda.pacientes.domain.model.gateway.PacienteGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Cambia la tarjeta de identidad (TI) por la cedula de ciudadania (CC) cuando el paciente cumple 18 anos.
 * Es la unica forma de cambiar el documento: la actualizacion general (PUT) no lo permite.
 */
@RequiredArgsConstructor
public class CambiarDocumentoUseCase {

    static final int MAYORIA_DE_EDAD = 18;
    private static final Pattern CEDULA = Pattern.compile("^[0-9]{5,20}$");

    private final PacienteGateway pacienteGateway;
    private final Clock reloj;

    public Paciente ejecutar(String codigo, String numeroCedula) {
        if (numeroCedula == null || !CEDULA.matcher(numeroCedula.trim()).matches()) {
            throw new IllegalArgumentException("El numero de cedula debe tener entre 5 y 20 digitos");
        }
        Paciente paciente = pacienteGateway.buscarPorCodigo(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException(codigo));
        if (!paciente.isActivo()) {
            throw new PacienteInactivoException(codigo);
        }
        if (paciente.getTipoDocumento() != TipoDocumento.TI) {
            throw new ConflictoPacienteException("DOCUMENTO_NO_ES_TI", "El paciente " + codigo + " tiene "
                    + paciente.getTipoDocumento() + ": solo se cambia la tarjeta de identidad por cedula");
        }
        int edad = Period.between(paciente.getFechaNacimiento(), LocalDate.now(reloj)).getYears();
        if (edad < MAYORIA_DE_EDAD) {
            throw new ReglaNegocioException("MENOR_DE_EDAD", "El paciente tiene " + edad
                    + " anos: la cedula se expide a partir de los " + MAYORIA_DE_EDAD);
        }
        String numero = numeroCedula.trim();
        if (pacienteGateway.existeDocumento(TipoDocumento.CC, numero)) {
            throw new PacienteDuplicadoException("Ya existe un paciente con el documento CC " + numero);
        }
        paciente.setTipoDocumento(TipoDocumento.CC);
        paciente.setNumeroDocumento(numero);
        return pacienteGateway.guardar(paciente);
    }
}
