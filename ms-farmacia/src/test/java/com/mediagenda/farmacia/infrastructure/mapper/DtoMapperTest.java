package com.mediagenda.farmacia.infrastructure.mapper;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import com.mediagenda.farmacia.domain.model.SolicitudItem;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.ItemPeticion;
import com.mediagenda.farmacia.infrastructure.entry_points.dto.MedicamentoPeticion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoMapperTest {

    private final DtoMapper mapper = new DtoMapper();

    @Test
    void conversiones() {
        assertThatThrownBy(() -> mapper.toDomain((MedicamentoPeticion) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toDomain(new MedicamentoPeticion("A", "B", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.toDomain(new MedicamentoPeticion("A", "B", 3)).getStock()).isEqualTo(3);
        assertThat(mapper.toDomain((List<ItemPeticion>) null)).isNull();
        List<SolicitudItem> items = mapper.toDomain(Arrays.asList(new ItemPeticion("MDT-0001", 1, null), null));
        assertThat(items.get(0).codigoMedicamento()).isEqualTo("MDT-0001");
        assertThat(items.get(1)).isNull();
    }

    @Test
    void filtros() {
        assertThat(DtoMapper.textoOpcional(" ")).isNull();
        assertThat(DtoMapper.textoOpcional(" PAC-1 ")).isEqualTo("PAC-1");
        assertThat(DtoMapper.estadoOpcional(null)).isNull();
        assertThat(DtoMapper.estadoOpcional("anulada")).isEqualTo(EstadoFormula.ANULADA);
        assertThatThrownBy(() -> DtoMapper.estadoOpcional("x")).isInstanceOf(IllegalArgumentException.class);
    }
}
