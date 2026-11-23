package com.mediagenda.farmacia.infrastructure.mapper;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.Formula;
import com.mediagenda.farmacia.domain.model.Medicamento;
import com.mediagenda.farmacia.domain.model.Pagina;
import com.mediagenda.farmacia.domain.model.SolicitudItem;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.FormulaRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.ItemPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.ItemRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.MedicamentoPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.MedicamentoRespuesta;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.PaginaRespuesta;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Component
public class DtoMapper {

    public Medicamento toDomain(MedicamentoPeticion p) {
        if (p == null) {
            throw new IllegalArgumentException("El cuerpo de la peticion es obligatorio");
        }
        if (p.stock() == null) {
            throw new IllegalArgumentException("El campo stock es obligatorio");
        }
        return Medicamento.builder().nombre(p.nombre()).presentacion(p.presentacion()).stock(p.stock()).build();
    }

    public List<SolicitudItem> toDomain(List<ItemPeticion> items) {
        return items == null ? null : items.stream()
                .map(i -> i == null ? null : new SolicitudItem(i.codigoMedicamento(), i.cantidad(), i.indicaciones()))
                .toList();
    }

    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static EstadoFormula estadoOpcional(String valor) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            return null;
        }
        return Arrays.stream(EstadoFormula.values())
                .filter(e -> e.name().equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado invalido: use EMITIDA, DISPENSADA o ANULADA"));
    }

    public MedicamentoRespuesta toRespuesta(Medicamento m) {
        return new MedicamentoRespuesta(m.getCodigo(), m.getNombre(), m.getPresentacion(), m.getStock(), m.isActivo());
    }

    public FormulaRespuesta toRespuesta(Formula f) {
        List<ItemRespuesta> items = f.getItems().stream()
                .map(i -> new ItemRespuesta(i.medicamento().getCodigo(), i.medicamento().getNombre(),
                        i.medicamento().getPresentacion(), i.cantidad(), i.indicaciones()))
                .toList();
        return new FormulaRespuesta(f.getCodigo(), f.getCodigoCita(), f.getCodigoPaciente(), f.getNombrePaciente(),
                f.getEstado().name(), f.getFechaEmision(), f.getFechaDispensacion(), items);
    }

    public <D, R> PaginaRespuesta<R> toRespuesta(Pagina<D> pagina, Function<D, R> conversion) {
        return new PaginaRespuesta<>(pagina.contenido().stream().map(conversion).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
