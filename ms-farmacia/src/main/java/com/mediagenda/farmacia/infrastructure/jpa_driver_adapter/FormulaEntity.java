package com.mediagenda.farmacia.infrastructure.jpa_driver_adapter;

import com.mediagenda.farmacia.domain.model.EstadoFormula;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "formulas")
@Getter
@Setter
@NoArgsConstructor
public class FormulaEntity {

    @Id
    @Column(name = "id_formula")
    private UUID idFormula;

    @Column(nullable = false, unique = true, length = 16)
    private String codigo;

    @Column(name = "codigo_cita", nullable = false, length = 16)
    private String codigoCita;

    @Column(name = "codigo_paciente", nullable = false, length = 20)
    private String codigoPaciente;

    @Column(name = "nombre_paciente", nullable = false, length = 170)
    private String nombrePaciente;

    @Column(name = "correo_paciente", nullable = false, length = 120)
    private String correoPaciente;

    @Column(name = "telefono_paciente", length = 20)
    private String telefonoPaciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoFormula estado;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_dispensacion")
    private LocalDateTime fechaDispensacion;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("idItem")
    private List<ItemFormulaEntity> items = new ArrayList<>();
}
