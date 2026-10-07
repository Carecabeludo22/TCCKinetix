package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
public class Servico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @Column(nullable = false, length = 120)
    public String nome;
    @Column(length = 500)
    public String descricao;
    @Column(name = "duracao_minutos", nullable = false)
    public int duracaoMinutos;
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;
    @Column(nullable = false)
    public boolean ativo = true;
    public Servico() {}
}
