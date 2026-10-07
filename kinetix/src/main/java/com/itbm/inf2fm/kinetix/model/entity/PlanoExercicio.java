package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.config.CriptografiaDadosConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "planos_exercicios")
public class PlanoExercicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id")
    public Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consulta_id")
    public Consulta consulta;
    @Column(nullable = false, length = 160)
    public String titulo;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(name = "orientacoes_enc", columnDefinition = "nvarchar(max)")
    public String orientacoes;
    @Column(nullable = false)
    public LocalDate inicio;
    public LocalDate fim;
    @Column(nullable = false)
    public boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();
    public PlanoExercicio() {}
}
