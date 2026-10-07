package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ClinicalService {
    private final ConsultaRepository consultas;
    private final ProntuarioRepository prontuarios;
    private final RegistroDorRepository registrosDor;
    private final PlanoExercicioRepository planos;
    private final ItemPrescricaoRepository itens;
    private final ExercicioRepository exercicios;
    private final FisioterapeutaRepository fisioterapeutas;
    private final PacienteRepository pacientes;
    private final AuditoriaAcessoSaudeRepository auditoria;
    private final NotificacaoService notificacoes;

    public ClinicalService(ConsultaRepository consultas, ProntuarioRepository prontuarios,
        RegistroDorRepository registrosDor, PlanoExercicioRepository planos, ItemPrescricaoRepository itens,
        ExercicioRepository exercicios, FisioterapeutaRepository fisioterapeutas, PacienteRepository pacientes,
        AuditoriaAcessoSaudeRepository auditoria, NotificacaoService notificacoes) {
        this.consultas = consultas; this.prontuarios = prontuarios; this.registrosDor = registrosDor;
        this.planos = planos; this.itens = itens; this.exercicios = exercicios;
        this.fisioterapeutas = fisioterapeutas; this.pacientes = pacientes;
        this.auditoria = auditoria; this.notificacoes = notificacoes;
    }

    @Transactional
    public ProntuarioResumo salvarProntuario(Long consultaId, Long fisioId, ProntuarioRequest request) {
        Consulta consulta = consultas.buscarDetalhesPorId(consultaId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
        if (!consulta.fisioterapeuta.usuario.id.equals(fisioId))
            throw ErroNegocio.proibido("Somente o fisioterapeuta responsável pode registrar o prontuário.");
        Prontuario p = prontuarios.findByConsulta_Id(consultaId).orElseGet(Prontuario::new);
        boolean novo = p.id == null;
        p.consulta = consulta; p.paciente = consulta.paciente; p.fisioterapeuta = consulta.fisioterapeuta;
        p.registroClinico = request.registroClinico();
        if (novo) p.criadoEm = OffsetDateTime.now();
        p.atualizadoEm = OffsetDateTime.now();
        prontuarios.save(p);
        auditar(fisioId, consulta.paciente, "PRONTUARIO", p.id, novo ? "CRIAR" : "ATUALIZAR");
        return resumo(p);
    }

    @Transactional
    public List<ProntuarioResumo> prontuarioPaciente(Long solicitanteId, PerfilUsuario perfil, Long pacienteId) {
        validarAcessoPaciente(solicitanteId, perfil, pacienteId);
        List<Prontuario> lista = perfil == PerfilUsuario.PACIENTE
            ? prontuarios.findByPaciente_Usuario_IdOrderByCriadoEmDesc(pacienteId)
            : prontuarios.findByPaciente_Usuario_IdAndFisioterapeuta_Usuario_IdOrderByCriadoEmDesc(pacienteId, solicitanteId);
        lista.forEach(p -> auditar(solicitanteId, p.paciente, "PRONTUARIO", p.id, "LEITURA"));
        return lista.stream().map(this::resumo).toList();
    }

    @Transactional
    public RegistroDorResumo registrarDor(Long pacienteId, RegistroDorRequest request, Long consultaId) {
        Paciente paciente = pacientes.findById(pacienteId)
            .orElseThrow(() -> ErroNegocio.proibido("O perfil atual não é de paciente."));
        Consulta consulta = null;
        if (consultaId != null) {
            consulta = consultas.findById(consultaId)
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
            if (!consulta.paciente.usuario.id.equals(pacienteId))
                throw ErroNegocio.proibido("A consulta informada não pertence ao paciente.");
        }
        RegistroDor registro = new RegistroDor();
        registro.paciente = paciente; registro.consulta = consulta; registro.escalaEva = request.escalaEva();
        registro.observacao = request.observacao();
        registro.registradoEm = request.registradoEm() == null ? OffsetDateTime.now() : request.registradoEm();
        registrosDor.save(registro);
        auditar(pacienteId, paciente, "DIARIO_DOR", registro.id, "CRIAR");
        return resumo(registro);
    }

    @Transactional
    public List<RegistroDorResumo> diarioDor(Long solicitanteId, PerfilUsuario perfil, Long pacienteId) {
        validarAcessoPaciente(solicitanteId, perfil, pacienteId);
        List<RegistroDor> lista = registrosDor.findByPaciente_Usuario_IdOrderByRegistradoEmDesc(pacienteId);
        lista.forEach(r -> auditar(solicitanteId, r.paciente, "DIARIO_DOR", r.id, "LEITURA"));
        return lista.stream().map(this::resumo).toList();
    }

    @Transactional
    public PrescricaoResumo prescrever(Long fisioId, Long pacienteId, PrescricaoRequest request) {
        if (!consultas.existeVinculoClinico(fisioId, pacienteId))
            throw ErroNegocio.proibido("A prescrição exige um vínculo de atendimento com o paciente.");
        Fisioterapeuta fisio = fisioterapeutas.findByUsuario_Id(fisioId)
            .orElseThrow(() -> ErroNegocio.proibido("Perfil de fisioterapeuta não encontrado."));
        Paciente paciente = pacientes.findById(pacienteId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Paciente não encontrado."));
        Consulta consulta = null;
        if (request.consultaId() != null) {
            consulta = consultas.findById(request.consultaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
            if (!consulta.paciente.usuario.id.equals(pacienteId) || !consulta.fisioterapeuta.usuario.id.equals(fisioId))
                throw ErroNegocio.proibido("A consulta não pertence a este paciente e fisioterapeuta.");
        }
        if (request.fim() != null && request.fim().isBefore(request.inicio()))
            throw ErroNegocio.invalido("A data final da prescrição não pode anteceder o início.");
        PlanoExercicio plano = new PlanoExercicio();
        plano.fisioterapeuta = fisio; plano.paciente = paciente; plano.consulta = consulta;
        plano.titulo = request.titulo(); plano.orientacoes = request.orientacoes();
        plano.inicio = request.inicio(); plano.fim = request.fim(); plano.ativo = true;
        planos.save(plano);
        for (ItemPrescricaoRequest r : request.itens()) {
            Exercicio exercicio = exercicios.findByIdAndAtivoTrue(r.exercicioId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Exercício não encontrado ou inativo."));
            ItemPrescricao item = new ItemPrescricao();
            item.plano = plano; item.exercicio = exercicio; item.ordem = r.ordem();
            item.series = r.series(); item.repeticoes = r.repeticoes(); item.duracaoSegundos = r.duracaoSegundos();
            item.frequencia = r.frequencia(); item.orientacoes = r.orientacoes(); itens.save(item);
        }
        notificacoes.notificarPrescricao(plano);
        auditar(fisioId, paciente, "PRESCRICAO", plano.id, "CRIAR");
        return resumo(plano);
    }

    @Transactional
    public NotificacaoResumo programarLembreteExercicio(Long pacienteId, Long planoId, OffsetDateTime enviarEm) {
        PlanoExercicio plano = planos.findByIdAndPaciente_Usuario_IdAndAtivoTrue(planoId,pacienteId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Rotina ativa não encontrada."));
        return notificacoes.agendarLembreteExercicio(plano,enviarEm);
    }

    @Transactional
    public List<PrescricaoResumo> prescricoesPaciente(Long pacienteId) {
        List<PlanoExercicio> lista = planos.findByPaciente_Usuario_IdAndAtivoTrueOrderByInicioDesc(pacienteId);
        lista.forEach(p -> auditar(p.paciente.usuario.id, p.paciente, "PRESCRICAO", p.id, "LEITURA"));
        return lista.stream().map(this::resumo).toList();
    }

    private void validarAcessoPaciente(Long solicitanteId, PerfilUsuario perfil, Long pacienteId) {
        if (perfil == PerfilUsuario.PACIENTE) {
            if (!solicitanteId.equals(pacienteId)) throw ErroNegocio.proibido("Você só pode acessar seus próprios dados clínicos.");
        } else if (perfil == PerfilUsuario.FISIO) {
            if (!consultas.existeVinculoClinico(solicitanteId, pacienteId))
                throw ErroNegocio.proibido("O profissional não possui vínculo clínico com este paciente.");
        } else throw ErroNegocio.proibido("Este perfil não pode acessar dados clínicos.");
    }

    private void auditar(Long atorId, Paciente paciente, String recurso, Long recursoId, String acao) {
        AuditoriaAcessoSaude registro = new AuditoriaAcessoSaude();
        registro.ator = new Usuario(); registro.ator.id = atorId; registro.paciente = paciente;
        registro.recurso = recurso; registro.recursoId = recursoId; registro.acao = acao; auditoria.save(registro);
    }

    private ProntuarioResumo resumo(Prontuario p) {
        return new ProntuarioResumo(p.id, p.consulta.id, p.paciente.usuario.id, p.fisioterapeuta.usuario.id,
            p.registroClinico, p.criadoEm, p.atualizadoEm);
    }

    private RegistroDorResumo resumo(RegistroDor r) {
        return new RegistroDorResumo(r.id, r.paciente.usuario.id, r.consulta == null ? null : r.consulta.id,
            r.escalaEva, r.observacao, r.registradoEm);
    }

    private PrescricaoResumo resumo(PlanoExercicio p) {
        List<ItemPrescricaoResumo> items = itens.findByPlano_IdOrderByOrdem(p.id).stream()
            .map(i -> new ItemPrescricaoResumo(i.id, i.exercicio.id, i.exercicio.nome, i.ordem,
                i.series, i.repeticoes, i.duracaoSegundos, i.frequencia, i.orientacoes)).toList();
        return new PrescricaoResumo(p.id, p.fisioterapeuta.usuario.id, p.paciente.usuario.id,
            p.consulta == null ? null : p.consulta.id, p.titulo, p.orientacoes, p.inicio, p.fim, p.ativo, items);
    }
}
