package com.itbm.inf2fm.kinetix.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fisioterapeutas")
public class Fisioterapeuta {
    @Id @Column(name = "usuario_id")
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId @JoinColumn(name = "usuario_id")
    public Usuario usuario;
    @Column(name = "crefito_numero", nullable = false, length = 30)
    public String crefitoNumero;
    @Column(name = "crefito_uf", nullable = false, length = 2)
    public String crefitoUf;
    @Column(name = "crefito_verificado", nullable = false)
    public boolean crefitoVerificado;
    @Column(name = "nome_clinica", length = 160)
    public String nomeClinica;
    @Column(name = "apresentacao", length = 1200)
    public String apresentacao;
    @Column(length = 160)
    public String logradouro;
    @Column(length = 20)
    public String numero;
    @Column(length = 100)
    public String complemento;
    @Column(length = 100)
    public String bairro;
    @Column(nullable = false, length = 100)
    public String cidade;
    @Column(nullable = false, length = 2)
    public String uf;
    @Column(length = 9)
    public String cep;
    @Column(precision = 9, scale = 6)
    public BigDecimal latitude;
    @Column(precision = 9, scale = 6)
    public BigDecimal longitude;
    @Column(name = "atende_online", nullable = false)
    public boolean atendeOnline;
    @Column(nullable = false)
    public boolean ativo = true;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "fisioterapeuta_especialidades",
        joinColumns = @JoinColumn(name = "fisioterapeuta_id"),
        inverseJoinColumns = @JoinColumn(name = "especialidade_id"))
    public Set<Especialidade> especialidades = new HashSet<>();

    protected Fisioterapeuta() {}
    public Fisioterapeuta(Usuario usuario) { this.usuario = usuario; }
}
