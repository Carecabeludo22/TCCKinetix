package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bloqueios_agenda")
public class BloqueioAgenda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @Column(nullable = false)
    public OffsetDateTime inicio;
    @Column(nullable = false)
    public OffsetDateTime fim;
    @Column(length = 300)
    public String motivo;
    public BloqueioAgenda() {}
}
