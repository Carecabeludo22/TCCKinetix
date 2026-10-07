package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilidades_semanais")
public class DisponibilidadeSemanal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @Column(name = "dia_semana", nullable = false)
    public int diaSemana;
    @Column(name = "hora_inicio", nullable = false)
    public LocalTime horaInicio;
    @Column(name = "hora_fim", nullable = false)
    public LocalTime horaFim;
    @Column(name = "vigencia_inicio", nullable = false)
    public LocalDate vigenciaInicio;
    @Column(name = "vigencia_fim")
    public LocalDate vigenciaFim;
    @Column(nullable = false)
    public boolean ativa = true;
    public DisponibilidadeSemanal() {}
}
