package com.mediagenda.citas.infrastructure.rest_client;

import com.mediagenda.citas.domain.model.DatosPaciente;
import com.mediagenda.citas.domain.model.exception.ServicioNoDisponibleException;
import com.mediagenda.citas.domain.model.gateway.PacienteGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Llamada sincrona a ms-pacientes con RestClient. 404 significa que el paciente no existe;
 * cualquier otro error o un tiempo de espera agotado se convierte en 503 (servicio no disponible).
 */
public class PacienteRestAdapter implements PacienteGateway {

    private final RestClient restClient;

    public PacienteRestAdapter(RestClient.Builder builder, String urlBase) {
        this.restClient = builder.baseUrl(urlBase).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PacienteRemoto(String codigo, String nombres, String apellidos, String correo, String telefono,
                          boolean activo) {
    }

    @Override
    public Optional<DatosPaciente> buscar(String codigoPaciente) {
        try {
            return restClient.get()
                    .uri("/api/v1/pacientes/{codigo}", codigoPaciente)
                    .exchange((peticion, respuesta) -> {
                        int estado = respuesta.getStatusCode().value();
                        if (estado == 404) {
                            return Optional.empty();
                        }
                        if (estado != 200) {
                            throw new ServicioNoDisponibleException(
                                    "ms-pacientes respondio con estado " + estado, null);
                        }
                        PacienteRemoto p = respuesta.bodyTo(PacienteRemoto.class);
                        return Optional.of(new DatosPaciente(p.codigo(),
                                p.nombres() + " " + p.apellidos(), p.correo(), p.telefono(), p.activo()));
                    });
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible comunicarse con ms-pacientes", e);
        }
    }
}
