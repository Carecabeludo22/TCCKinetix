package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente {
    @Id @Column(name = "usuario_id")
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId @JoinColumn(name = "usuario_id")
    public Usuario usuario;
    @Column(name = "data_nascimento")
    public LocalDate dataNascimento;
    @Column(length = 100)
    public String cidade;
    @Column(length = 100)
    public String bairro;
    @Column(length = 2)
    public String uf;
    protected Paciente() {}
    public Paciente(Usuario usuario) { this.usuario = usuario; }
}
