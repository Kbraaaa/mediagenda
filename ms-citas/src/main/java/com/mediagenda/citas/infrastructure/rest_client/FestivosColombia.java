package com.mediagenda.citas.infrastructure.rest_client;

import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.TreeSet;

/**
 * Plan B si Nager.Date no responde: calcula los festivos de Colombia segun la Ley 51 de 1983
 * ("Ley Emiliani"). Tres grupos:
 * 1) fijos: 1 ene, 1 may, 20 jul, 7 ago, 8 dic, 25 dic;
 * 2) trasladables al lunes siguiente: Reyes, San Jose, San Pedro y San Pablo, Asuncion, Dia de la Raza,
 *    Todos los Santos, Independencia de Cartagena;
 * 3) dependientes de la Pascua: Jueves y Viernes Santo (fijos) y Ascension, Corpus Christi y
 *    Sagrado Corazon (trasladados al lunes).
 */
@UtilityClass
public class FestivosColombia {

    public static Set<LocalDate> calcular(int anio) {
        Set<LocalDate> festivos = new TreeSet<>();
        festivos.add(LocalDate.of(anio, 1, 1));
        festivos.add(LocalDate.of(anio, 5, 1));
        festivos.add(LocalDate.of(anio, 7, 20));
        festivos.add(LocalDate.of(anio, 8, 7));
        festivos.add(LocalDate.of(anio, 12, 8));
        festivos.add(LocalDate.of(anio, 12, 25));

        festivos.add(lunesSiguiente(LocalDate.of(anio, 1, 6)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 3, 19)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 6, 29)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 8, 15)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 10, 12)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 11, 1)));
        festivos.add(lunesSiguiente(LocalDate.of(anio, 11, 11)));

        LocalDate pascua = domingoDePascua(anio);
        festivos.add(pascua.minusDays(3));
        festivos.add(pascua.minusDays(2));
        festivos.add(lunesSiguiente(pascua.plusDays(39)));
        festivos.add(lunesSiguiente(pascua.plusDays(60)));
        festivos.add(lunesSiguiente(pascua.plusDays(68)));
        return festivos;
    }

    /** Si la fecha ya es lunes se queda; si no, pasa al lunes siguiente. */
    static LocalDate lunesSiguiente(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    /** Domingo de Pascua (calendario gregoriano), algoritmo de Meeus/Jones/Butcher. */
    static LocalDate domingoDePascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
