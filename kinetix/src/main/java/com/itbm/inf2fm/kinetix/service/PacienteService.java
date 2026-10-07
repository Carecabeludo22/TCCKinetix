package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.Paciente;
import com.itbm.inf2fm.kinetix.model.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PacienteService {
    private final PacienteRepository pacientes;
    public PacienteService(PacienteRepository pacientes) { this.pacientes=pacientes; }

    @Transactional(readOnly=true)
    public PacientePerfilResumo meuPerfil(Long usuarioId) {
        Paciente p=pacientes.findById(usuarioId)
            .orElseThrow(()->ErroNegocio.naoEncontrado("Perfil de paciente não encontrado."));
        return resumo(p);
    }

    @Transactional
    public PacientePerfilResumo atualizarPerfil(Long usuarioId, PerfilPacienteRequest request) {
        Paciente p=pacientes.findById(usuarioId)
            .orElseThrow(()->ErroNegocio.naoEncontrado("Perfil de paciente não encontrado."));
        if (request.nome()!=null) p.usuario.nome=request.nome().trim();
        if (request.telefone()!=null) p.usuario.telefone=request.telefone();
        if (request.documentoIdentidade()!=null) p.usuario.documentoIdentidade=request.documentoIdentidade();
        if (request.fotoUrl()!=null) p.usuario.fotoUrl=request.fotoUrl();
        if (request.dataNascimento()!=null) p.dataNascimento=request.dataNascimento();
        if (request.cidade()!=null) p.cidade=request.cidade();
        if (request.bairro()!=null) p.bairro=request.bairro();
        if (request.uf()!=null) p.uf=request.uf().toUpperCase();
        return resumo(p);
    }

    private PacientePerfilResumo resumo(Paciente p) {
        return new PacientePerfilResumo(p.usuario.id,p.usuario.nome,p.usuario.email,p.usuario.telefone,
            p.usuario.documentoIdentidade,p.usuario.fotoUrl,p.dataNascimento,p.cidade,p.bairro,p.uf);
    }
}
