package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.config.CriptografiaDadosConverter;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registros_dor")
public class RegistroDor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id")
    public Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consulta_id")
    public Consulta consulta;
    @Column(name = "escala_eva", nullable = false)
    public int escalaEva;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(name = "observacao_enc", columnDefinition = "nvarchar(max)")
    public String observacao;
    @Column(name = "registrado_em", nullable = false)
    public OffsetDateTime registradoEm = OffsetDateTime.now();
    public RegistroDor() {}
}
