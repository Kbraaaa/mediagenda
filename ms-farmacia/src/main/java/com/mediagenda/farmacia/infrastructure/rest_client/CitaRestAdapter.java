package com.mediagenda.farmacia.infrastructure.rest_client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediagenda.farmacia.domain.model.DatosCita;
import com.mediagenda.farmacia.domain.model.exception.ServicioNoDisponibleException;
import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Llamada sincrona a ms-citas con RestClient: 404 = la cita no existe; otro error = 503. */
public class CitaRestAdapter implements CitaGateway {

    private final RestClient restClient;

    public CitaRestAdapter(RestClient.Builder builder, String urlBase) {
        this.restClient = builder.baseUrl(urlBase).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CitaRemota(String codigo, String codigoPaciente, String nombrePaciente, String correoPaciente,
                      String telefonoPaciente, String estado) {
    }

    @Override
    public Optional<DatosCita> buscar(String codigoCita) {
        try {
            return restClient.get()
                    .uri("/api/v1/citas/{codigo}", codigoCita)
                    .exchange((peticion, respuesta) -> {
                        int estado = respuesta.getStatusCode().value();
                        if (estado == 404) {
                            return Optional.empty();
                        }
                        if (estado != 200) {
                            throw new ServicioNoDisponibleException("ms-citas respondio con estado " + estado, null);
                        }
                        CitaRemota c = respuesta.bodyTo(CitaRemota.class);
                        return Optional.of(new DatosCita(c.codigo(), c.codigoPaciente(), c.nombrePaciente(),
                                c.correoPaciente(), c.telefonoPaciente(), c.estado()));
                    });
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No fue posible comunicarse con ms-citas", e);
        }
    }
}
