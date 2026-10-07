package com.itbm.inf2fm.kinetix.model.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity
@Table(name="assinaturas")
public class Assinatura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="fisioterapeuta_id") public Fisioterapeuta fisioterapeuta;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="plano_id") public PlanoAssinatura plano;
    @Column(nullable=false) public LocalDate inicio;
    public LocalDate fim;
    @Column(nullable=false,length=20) public String status="PENDENTE";
    public Assinatura() {}
}
