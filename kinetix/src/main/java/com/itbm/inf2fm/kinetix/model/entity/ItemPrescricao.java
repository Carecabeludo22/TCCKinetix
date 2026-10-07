package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.config.CriptografiaDadosConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "itens_prescricao")
public class ItemPrescricao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plano_id")
    public PlanoExercicio plano;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "exercicio_id")
    public Exercicio exercicio;
    @Column(nullable = false)
    public int ordem;
    public Integer series;
    public Integer repeticoes;
    @Column(name = "duracao_segundos")
    public Integer duracaoSegundos;
    @Column(length = 160)
    public String frequencia;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(name = "orientacoes_enc", columnDefinition = "nvarchar(max)")
    public String orientacoes;
    public ItemPrescricao() {}
}
