package com.mediagenda.farmacia;

import com.mediagenda.farmacia.domain.model.DatosCita;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.ItemFormula;
import com.mediagenda.farmacia.domain.model.Medicamento;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Datos {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));
    public static final UUID ID_ACETAMINOFEN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ID_IBUPROFENO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private Datos() {
    }

    public static Medicamento acetaminofen() {
        return Medicamento.builder().idMedicamento(ID_ACETAMINOFEN).codigo("MDT-0001").nombre("Acetaminofen")
                .presentacion("Tabletas 500 mg").stock(500).activo(true).build();
    }

    public static Medicamento ibuprofeno() {
        return Medicamento.builder().idMedicamento(ID_IBUPROFENO).codigo("MDT-0002").nombre("Ibuprofeno")
                .presentacion("Tabletas 400 mg").stock(3).activo(true).build();
    }

    public static DatosCita citaAtendida() {
        return new DatosCita("CIT-2026-000001", "PAC-000001", "Laura Gomez", "laura@correo.com", "3101234567", "ATENDIDA");
    }

    public static Formula formula() {
        return Formula.builder()
                .idFormula(UUID.fromString("ffffffff-0000-0000-0000-000000000001"))
                .codigo("FOR-2026-000001")
                .codigoCita("CIT-2026-000001")
                .codigoPaciente("PAC-000001")
                .nombrePaciente("Laura Gomez")
                .correoPaciente("laura@correo.com")
                .telefonoPaciente("3101234567")
                .estado(EstadoFormula.EMITIDA)
                .fechaEmision(LocalDateTime.of(2026, 11, 20, 10, 0))
                .items(new ArrayList<>(List.of(
                        new ItemFormula(1L, acetaminofen(), 20, "1 cada 8 horas"),
                        new ItemFormula(2L, ibuprofeno(), 2, null))))
                .build();
    }
}
