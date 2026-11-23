package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "medicamentos")
@Getter
@Setter
@NoArgsConstructor
public class MedicamentoEntity {

    @Id
    @Column(name = "id_medicamento")
    private UUID idMedicamento;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String presentacion;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean activo;
}
