package com.mediagenda.citas.infrastructure.rest_client;

import com.mediagenda.citas.domain.model.gateway.FestivoGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * API externa Nager.Date: festivos de Colombia. Se consulta UNA vez por anio y se guarda en memoria,
 * asi que no afecta el tiempo de respuesta. Si la API no responde, se usa el calculo local de la
 * Ley 51 de 1983 (FestivosColombia): la validacion de festivos nunca depende de que la API este arriba.
 */
public class FestivosNagerAdapter implements FestivoGateway {

    private static final Logger log = LoggerFactory.getLogger(FestivosNagerAdapter.class);

    private final RestClient restClient;
    private final String pais;
    private final Map<Integer, Set<LocalDate>> cache = new ConcurrentHashMap<>();

    public FestivosNagerAdapter(RestClient.Builder builder, String urlBase, String pais) {
        this.restClient = builder.baseUrl(urlBase).build();
        this.pais = pais;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FestivoNager(LocalDate date, String localName) {
    }

    /** Carga los festivos de esos anios en memoria (se llama al arrancar, fuera de cualquier peticion). */
    public void precargar(int... anios) {
        for (int anio : anios) {
            cache.put(anio, obtener(anio));
        }
    }

    /** Primero la API externa; si falla o no trae datos, el calculo local. */
    private Set<LocalDate> obtener(int anio) {
        Set<LocalDate> festivos = consultar(anio);
        if (festivos == null || festivos.isEmpty()) {
            log.warn("Festivos de {}: se usa el calculo local (Ley 51 de 1983)", anio);
            return FestivosColombia.calcular(anio);
        }
        return festivos;
    }

    @Override
    public boolean esFestivo(LocalDate fecha) {
        return cache.computeIfAbsent(fecha.getYear(), this::obtener).contains(fecha);
    }

    private Set<LocalDate> consultar(int anio) {
        try {
            FestivoNager[] respuesta = restClient.get()
                    .uri("/api/v3/PublicHolidays/{anio}/{pais}", anio, pais)
                    .retrieve()
                    .body(FestivoNager[].class);
            if (respuesta == null) {
                return null;
            }
            return Arrays.stream(respuesta).map(FestivoNager::date).collect(Collectors.toUnmodifiableSet());
        } catch (RuntimeException e) {
            log.warn("No se pudieron consultar los festivos de {} en Nager.Date: {}", anio, e.getMessage());
            return null;
        }
    }
}
