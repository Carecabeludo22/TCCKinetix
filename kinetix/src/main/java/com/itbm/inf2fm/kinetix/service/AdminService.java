package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {
    private final EspecialidadeRepository especialidades;
    private final ExercicioRepository exercicios;
    private final PlanoAssinaturaRepository planos;
    private final AssinaturaRepository assinaturas;
    private final FisioterapeutaRepository fisioterapeutas;

    public AdminService(EspecialidadeRepository especialidades, ExercicioRepository exercicios,
        PlanoAssinaturaRepository planos, AssinaturaRepository assinaturas, FisioterapeutaRepository fisioterapeutas) {
        this.especialidades=especialidades; this.exercicios=exercicios; this.planos=planos;
        this.assinaturas=assinaturas; this.fisioterapeutas=fisioterapeutas;
    }

    @Transactional
    public EspecialidadeResumo criarEspecialidade(EspecialidadeRequest request) {
        if (especialidades.existsByNomeIgnoreCase(request.nome().trim()))
            throw ErroNegocio.conflito("Já existe uma especialidade com este nome.");
        Especialidade item = new Especialidade(); item.nome=request.nome().trim(); item.descricao=request.descricao();
        especialidades.save(item);
        return new EspecialidadeResumo(item.id,item.nome,item.descricao);
    }

    @Transactional
    public Long criarExercicio(ExercicioRequest request) {
        Exercicio item=new Exercicio(); item.nome=request.nome().trim(); item.descricao=request.descricao();
        item.videoUrl=request.videoUrl(); return exercicios.save(item).id;
    }

    @Transactional
    public Long criarPlano(PlanoAssinaturaRequest request) {
        if (request.precoMensal()==null || request.precoMensal().signum()<0)
            throw ErroNegocio.invalido("Informe um preço mensal não negativo.");
        PlanoAssinatura plano=new PlanoAssinatura(); plano.nome=request.nome().trim();
        plano.descricao=request.descricao(); plano.precoMensal=request.precoMensal();
        return planos.save(plano).id;
    }

    @Transactional
    public AssinaturaResumo solicitarAssinatura(Long usuarioId, Long planoId) {
        Fisioterapeuta fisio=fisioterapeutas.findByUsuario_Id(usuarioId)
            .orElseThrow(()->ErroNegocio.proibido("Somente fisioterapeutas podem solicitar uma assinatura."));
        PlanoAssinatura plano=planos.findById(planoId).filter(p->p.ativo)
            .orElseThrow(()->ErroNegocio.naoEncontrado("Plano ativo não encontrado."));
        Assinatura assinatura=new Assinatura(); assinatura.fisioterapeuta=fisio;
        assinatura.plano=plano; assinatura.inicio=LocalDate.now(); assinatura.status="PENDENTE";
        assinaturas.save(assinatura);
        return resumir(assinatura);
    }

    @Transactional(readOnly=true)
    public List<AssinaturaResumo> minhasAssinaturas(Long usuarioId) {
        return assinaturas.findByFisioterapeuta_Usuario_IdOrderByInicioDesc(usuarioId).stream().map(this::resumir).toList();
    }

    private AssinaturaResumo resumir(Assinatura a) {
        return new AssinaturaResumo(a.id,a.plano.id,a.plano.nome,a.status,a.inicio,a.fim);
    }
}
