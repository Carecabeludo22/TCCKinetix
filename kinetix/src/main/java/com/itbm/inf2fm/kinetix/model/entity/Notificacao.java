package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id")
    public Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consulta_id")
    public Consulta consulta;
    @Column(nullable = false, length = 120)
    public String titulo;
    @Column(nullable = false, length = 300)
    public String mensagem;
    @Column(name = "enviar_em", nullable = false)
    public OffsetDateTime enviarEm;
    @Column(nullable = false, length = 16)
    public String status = "PENDENTE";
    @Column(name = "enviado_em")
    public OffsetDateTime enviadoEm;
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();
    public Notificacao() {}
}
