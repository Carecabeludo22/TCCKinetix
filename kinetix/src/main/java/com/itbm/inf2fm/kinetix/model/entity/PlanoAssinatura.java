package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "planos_assinatura")
public class PlanoAssinatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 100)
    public String nome;
    @Column(length = 500)
    public String descricao;
    @Column(name = "preco_mensal", nullable = false, precision = 10, scale = 2)
    public BigDecimal precoMensal;
    @Column(nullable = false)
    public boolean ativo = true;
    public PlanoAssinatura() {}
}
