package com.itbm.inf2fm.kinetix.model.entity;

import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 120)
    public String nome;
    @Column(nullable = false, unique = true, length = 254)
    public String email;
    @Column(name = "senha_hash", nullable = false, length = 100)
    public String senhaHash;
    @Column(length = 30)
    public String telefone;
    @Column(name = "documento_identidade", length = 30)
    public String documentoIdentidade;
    @Column(name = "foto_url", length = 500)
    public String fotoUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    public PerfilUsuario perfil;
    @Column(nullable = false)
    public boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm = OffsetDateTime.now();

    public Usuario() {}
}
