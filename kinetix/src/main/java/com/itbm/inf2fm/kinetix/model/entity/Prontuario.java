package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.config.CriptografiaDadosConverter;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "prontuarios")
public class Prontuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consulta_id")
    public Consulta consulta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id")
    public Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fisioterapeuta_id")
    public Fisioterapeuta fisioterapeuta;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(name = "registro_clinico_enc", nullable = false, columnDefinition = "nvarchar(max)")
    public String registroClinico;
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    public OffsetDateTime atualizadoEm = OffsetDateTime.now();
    @PreUpdate void atualizarData() { atualizadoEm = OffsetDateTime.now(); }
    public Prontuario() {}
}
