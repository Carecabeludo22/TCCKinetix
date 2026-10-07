package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidades")
public class Especialidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 100)
    public String nome;
    @Column(length = 500)
    public String descricao;
    @Column(nullable = false)
    public boolean ativa = true;
    public Especialidade() {}
}
