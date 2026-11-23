package com.mediagenda.farmacia.domain.usecase;

import com.mediagenda.farmacia.domain.model.Codigos;
import com.mediagenda.farmacia.domain.model.DatosCita;
import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.EventoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.ItemFormula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.SolicitudItem;
import com.mediagenda.farmacia.domain.model.exception.ConflictoException;
import com.mediagenda.farmacia.domain.model.exception.NoEncontradoException;
import com.mediagenda.farmacia.domain.model.gateway.CitaGateway;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import com.mediagenda.farmacia.domain.model.gateway.FormulaGateway;
import com.mediagenda.farmacia.domain.model.gateway.MedicamentoGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Emite la formula de una cita. Reglas: la cita debe existir en ms-citas y estar ATENDIDA, solo puede
 * haber una formula activa por cita, y cada medicamento debe existir y estar activo. Emitir NO descuenta
 * stock: eso pasa al dispensar.
 */
@RequiredArgsConstructor
public class EmitirFormulaUseCase {

    public static final int MAXIMO_ITEMS = 10;

    private final CitaGateway citas;
    private final MedicamentoGateway medicamentos;
    private final FormulaGateway formulas;
    private final EventoGateway eventos;
    private final Clock reloj;

    public Formula ejecutar(String codigoCita, List<SolicitudItem> solicitud) {
        Validaciones.texto(codigoCita, "codigoCita", 1, 20);
        validarItems(solicitud);

        DatosCita cita = citas.buscar(codigoCita.trim()).orElseThrow(() ->
                new NoEncontradoException("CITA_NO_ENCONTRADA", "No existe la cita " + codigoCita));
        if (!cita.atendida()) {
            throw new ConflictoException("CITA_NO_ATENDIDA",
                    "Solo se formula sobre citas ATENDIDA; la cita " + codigoCita + " esta " + cita.estado());
        }
        if (formulas.existeActivaParaCita(cita.codigo())) {
            throw new ConflictoException("FORMULA_EXISTENTE", "La cita " + cita.codigo() + " ya tiene una formula");
        }

        List<ItemFormula> items = new ArrayList<>();
        for (SolicitudItem s : solicitud) {
            Medicamento medicamento = medicamentos.buscarPorCodigo(s.codigoMedicamento().trim()).orElseThrow(() ->
                    new NoEncontradoException("MEDICAMENTO_NO_ENCONTRADO",
                            "No existe el medicamento " + s.codigoMedicamento()));
            if (!medicamento.isActivo()) {
                throw new ConflictoException("MEDICAMENTO_INACTIVO",
                        "El medicamento " + medicamento.getCodigo() + " no esta disponible");
            }
            String indicaciones = s.indicaciones() == null || s.indicaciones().isBlank() ? null : s.indicaciones().trim();
            items.add(new ItemFormula(null, medicamento, s.cantidad(), indicaciones));
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Formula formula = Formula.builder()
                .idFormula(UUID.randomUUID())
                .codigo(Codigos.formula(ahora.getYear(), formulas.siguienteConsecutivo()))
                .codigoCita(cita.codigo())
                .codigoPaciente(cita.codigoPaciente())
                .nombrePaciente(cita.nombrePaciente())
                .correoPaciente(cita.correoPaciente())
                .telefonoPaciente(cita.telefonoPaciente())
                .estado(EstadoFormula.EMITIDA)
                .fechaEmision(ahora)
                .items(items)
                .build();
        Formula guardada = formulas.guardar(formula);
        eventos.publicar(EventoFormula.de(EventoFormula.EMITIDA, guardada,
                "Su formula esta lista para reclamar en la farmacia"));
        return guardada;
    }

    private static void validarItems(List<SolicitudItem> solicitud) {
        if (solicitud == null || solicitud.isEmpty()) {
            throw new IllegalArgumentException("La formula debe tener al menos un medicamento");
        }
        if (solicitud.size() > MAXIMO_ITEMS) {
            throw new IllegalArgumentException("La formula no puede tener mas de " + MAXIMO_ITEMS + " medicamentos");
        }
        Set<String> vistos = new HashSet<>();
        for (SolicitudItem s : solicitud) {
            if (s == null) {
                throw new IllegalArgumentException("Hay un medicamento vacio en la formula");
            }
            Validaciones.texto(s.codigoMedicamento(), "codigoMedicamento", 1, 20);
            Validaciones.rango(s.cantidad(), "cantidad", 1, 100);
            if (s.indicaciones() != null && s.indicaciones().length() > 300) {
                throw new IllegalArgumentException("Las indicaciones no pueden superar 300 caracteres");
            }
            if (!vistos.add(s.codigoMedicamento().trim().toUpperCase())) {
                throw new IllegalArgumentException("El medicamento " + s.codigoMedicamento() + " esta repetido");
            }
        }
    }
}
