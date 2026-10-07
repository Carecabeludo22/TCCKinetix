package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.model.enums.StatusConsulta;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "consultas")
public class Consulta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id")
    public Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "servico_id")
    public Servico servico;
    @Column(nullable = false)
    public OffsetDateTime inicio;
    @Column(nullable = false)
    public OffsetDateTime fim;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    public StatusConsulta status = StatusConsulta.CONFIRMADA;
    @Column(name = "reserva_ativa", nullable = false)
    public boolean reservaAtiva = true;
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;
    @Column(name = "duracao_minutos", nullable = false)
    public int duracaoMinutos;
    @Column(name = "motivo_cancelamento", length = 500)
    public String motivoCancelamento;
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    public OffsetDateTime atualizadoEm = OffsetDateTime.now();
    @PreUpdate void atualizarData() { atualizadoEm = OffsetDateTime.now(); }
    public Consulta() {}
}
