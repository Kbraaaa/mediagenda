package com.mediagenda.pacientes.domain.model;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas de validacion de los datos de un paciente. Viven en el dominio, no en el controlador.
 * Si algo no cumple, lanza IllegalArgumentException (se responde 400).
 */
@UtilityClass
public class ValidadorPaciente {

    private static final Pattern DOCUMENTO = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9+ ]{7,20}$");
    private static final Set<String> TIPOS_SANGRE = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final int EDAD_MAXIMA = 120;

    public static void validar(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Los datos del paciente son obligatorios");
        }
        if (paciente.getTipoDocumento() == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        validarPatron(paciente.getNumeroDocumento(), DOCUMENTO, "numero de documento",
                "debe tener entre 5 y 20 letras o numeros");
        validarTexto(paciente.getNombres(), "nombres", 80);
        validarTexto(paciente.getApellidos(), "apellidos", 80);
        validarPatron(paciente.getCorreo(), CORREO, "correo", "no tiene un formato valido");
        if (paciente.getCorreo().trim().length() > 120) {
            throw new IllegalArgumentException("El correo no puede superar 120 caracteres");
        }
        if (tieneValor(paciente.getTelefono()) && !TELEFONO.matcher(paciente.getTelefono().trim()).matches()) {
            throw new IllegalArgumentException("El telefono debe tener entre 7 y 20 digitos");
        }
        validarFechaNacimiento(paciente.getFechaNacimiento());
        if (tieneValor(paciente.getTipoSangre()) && !TIPOS_SANGRE.contains(paciente.getTipoSangre().trim())) {
            throw new IllegalArgumentException("Tipo de sangre invalido: use A+, A-, B+, B-, AB+, AB-, O+ u O-");
        }
        if (paciente.getAlergias() != null && paciente.getAlergias().length() > 500) {
            throw new IllegalArgumentException("Las alergias no pueden superar 500 caracteres");
        }
    }

    /** Los campos opcionales vacios o con solo espacios se tratan como no enviados. */
    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void validarTexto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        if (valor.trim().length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " no puede superar " + maximo + " caracteres");
        }
    }

    private static void validarPatron(String valor, Pattern patron, String campo, String regla) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        if (!patron.matcher(valor.trim()).matches()) {
            throw new IllegalArgumentException("El " + campo + " " + regla);
        }
    }

    private static void validarFechaNacimiento(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        if (fecha.isBefore(hoy.minusYears(EDAD_MAXIMA))) {
            throw new IllegalArgumentException("La fecha de nacimiento no es valida");
        }
    }
}
