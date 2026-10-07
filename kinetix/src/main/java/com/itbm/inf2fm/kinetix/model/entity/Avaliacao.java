package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consulta_id")
    public Consulta consulta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id")
    public Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @Column(nullable = false)
    public int nota;
    @Column(length = 1000)
    public String comentario;
    @Column(nullable = false, length = 16)
    public String status = "PENDENTE";
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();
    public Avaliacao() {}
}
