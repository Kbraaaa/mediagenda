package com.mediagenda.citas.domain.usecase;

import com.mediagenda.citas.domain.model.Codigos;
import com.mediagenda.citas.domain.model.Medico;
import com.mediagenda.citas.domain.model.exception.ConflictoException;
import com.mediagenda.citas.domain.model.gateway.MedicoGateway;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Registra un medico con su codigo MED-0001. */
@RequiredArgsConstructor
public class RegistrarMedicoUseCase {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MedicoGateway medicoGateway;

    public Medico ejecutar(Medico datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del medico son obligatorios");
        }
        ReglasAgenda.validarTexto(datos.getNombre(), "nombre", 3, 100);
        if (datos.getEspecialidad() == null) {
            throw new IllegalArgumentException("La especialidad es obligatoria");
        }
        ReglasAgenda.validarTexto(datos.getCorreo(), "correo", 5, 120);
        String correo = datos.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato valido");
        }
        if (medicoGateway.existeCorreo(correo)) {
            throw new ConflictoException("MEDICO_DUPLICADO", "Ya existe un medico con el correo " + correo);
        }
        Medico medico = datos.toBuilder()
                .idMedico(UUID.randomUUID())
                .codigo(Codigos.medico(medicoGateway.siguienteConsecutivo()))
                .nombre(datos.getNombre().trim())
                .correo(correo)
                .activo(true)
                .build();
        return medicoGateway.guardar(medico);
    }
}
