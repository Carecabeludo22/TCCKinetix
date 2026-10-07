package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exercicios")
public class Exercicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 120)
    public String nome;
    @Column(length = 1000)
    public String descricao;
    @Column(name = "video_url", length = 500)
    public String videoUrl;
    @Column(nullable = false)
    public boolean ativo = true;
    public Exercicio() {}
}
