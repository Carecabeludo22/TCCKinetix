
package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.model.enums.StatusConsulta;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConsultaService {
    private final FisioterapeutaRepository fisioterapeutas;
    private final PacienteRepository pacientes;
    private final ServicoRepository servicos;
    private final DisponibilidadeRepository disponibilidades;
    private final BloqueioAgendaRepository bloqueios;
    private final ConsultaRepository consultas;
    private final AvaliacaoRepository avaliacoes;
    private final NotificacaoService notificacaoService;
    private final ZoneId zoneId;

    public ConsultaService(FisioterapeutaRepository fisioterapeutas, PacienteRepository pacientes,
        ServicoRepository servicos, DisponibilidadeRepository disponibilidades,
        BloqueioAgendaRepository bloqueios, ConsultaRepository consultas,
        AvaliacaoRepository avaliacoes, NotificacaoService notificacaoService, Environment environment) {
        this.fisioterapeutas = fisioterapeutas;
        this.pacientes = pacientes;
        this.servicos = servicos;
        this.disponibilidades = disponibilidades;
        this.bloqueios = bloqueios;
        this.consultas = consultas;
        this.avaliacoes = avaliacoes;
        this.notificacaoService = notificacaoService;
        this.zoneId = ZoneId.of(environment.getProperty("kinetix.time-zone", "America/Sao_Paulo"));
    }

    @Transactional
    public ConsultaResumo agendar(Long pacienteId, ConsultaRequest request) {
        // A linha do profissional permanece bloqueada até o commit. Todas as reservas
        // do mesmo profissional passam por ela antes da consulta de sobreposição.
        Fisioterapeuta fisio = fisioterapeutas.bloquearPorUsuarioId(request.fisioterapeutaId())
            .filter(f -> f.ativo && f.crefitoVerificado && f.usuario.ativo)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Fisioterapeuta verificado não encontrado."));
        Paciente paciente = pacientes.findById(pacienteId)
            .orElseThrow(() -> ErroNegocio.proibido("O cadastro atual não pertence a um paciente."));
        Servico servico = servicos.findByIdAndFisioterapeuta_Usuario_IdAndAtivoTrue(request.servicoId(), fisio.usuario.id)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço ativo não encontrado para este profissional."));
        OffsetDateTime inicio = request.inicio();
        if (inicio.getSecond() != 0 || inicio.getNano() != 0)
            throw ErroNegocio.invalido("O horário deve começar em um minuto exato.");
        if (!inicio.isAfter(OffsetDateTime.now()))
            throw ErroNegocio.invalido("Escolha uma data futura.");
        OffsetDateTime fim = inicio.plusMinutes(servico.duracaoMinutos);
        if (!dentroDaDisponibilidade(fisio.usuario.id, inicio, fim, servico.duracaoMinutos))
            throw ErroNegocio.conflito("O horário não está dentro da disponibilidade publicada.");
        if (bloqueios.existsByFisioterapeuta_Usuario_IdAndInicioLessThanAndFimGreaterThan(fisio.usuario.id, fim, inicio))
            throw ErroNegocio.conflito("O profissional bloqueou este período.");
        if (consultas.existeConflito(fisio.usuario.id, inicio, fim))
            throw ErroNegocio.conflito("Este horário já foi reservado. Escolha outro horário.");

        Consulta consulta = new Consulta();
        consulta.paciente = paciente;
        consulta.fisioterapeuta = fisio;
        consulta.servico = servico;
        consulta.inicio = inicio;
        consulta.fim = fim;
        consulta.status = StatusConsulta.SOLICITADA;
        consulta.reservaAtiva = true;
        consulta.preco = servico.preco;
        consulta.duracaoMinutos = servico.duracaoMinutos;
        consultas.saveAndFlush(consulta);
        notificacaoService.agendarLembretes(consulta);
        return resumir(consulta);
    }

    @Transactional(readOnly = true)
    public List<OffsetDateTime> horariosDisponiveis(Long fisioId, Long servicoId, LocalDate de, LocalDate ate) {
        if (de == null || ate == null || de.isAfter(ate) || ate.isAfter(de.plusDays(30)))
            throw ErroNegocio.invalido("Informe um intervalo de até 30 dias.");
        Fisioterapeuta fisio = fisioterapeutas.buscarPerfilPublico(fisioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Fisioterapeuta verificado não encontrado."));
        Servico servico = servicos.findByIdAndFisioterapeuta_Usuario_IdAndAtivoTrue(servicoId, fisioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço não encontrado."));
        List<OffsetDateTime> slots = new ArrayList<>();
        for (LocalDate date = de; !date.isAfter(ate); date = date.plusDays(1)) {
            for (DisponibilidadeSemanal d : disponibilidades.findByFisioterapeuta_Usuario_IdAndDiaSemanaAndAtivaTrue(
                fisio.usuario.id, date.getDayOfWeek().getValue())) {
                if (date.isBefore(d.vigenciaInicio) || (d.vigenciaFim != null && date.isAfter(d.vigenciaFim))) continue;
                LocalTime hora = d.horaInicio;
                while (!hora.plusMinutes(servico.duracaoMinutos).isAfter(d.horaFim)) {
                    OffsetDateTime inicio = date.atTime(hora).atZone(zoneId).toOffsetDateTime();
                    OffsetDateTime fim = inicio.plusMinutes(servico.duracaoMinutos);
                    if (inicio.isAfter(OffsetDateTime.now()) &&
                        !bloqueios.existsByFisioterapeuta_Usuario_IdAndInicioLessThanAndFimGreaterThan(fisio.usuario.id, fim, inicio) &&
                        !consultas.existeConflito(fisio.usuario.id, inicio, fim)) slots.add(inicio);
                    hora = hora.plusMinutes(servico.duracaoMinutos);
                }
            }
        }
        return slots.stream().distinct().sorted().toList();
    }

    @Transactional
    public void bloquearAgenda(Long fisioId, BloqueioRequest request) {
        Fisioterapeuta fisio = fisioterapeutas.bloquearPorUsuarioId(fisioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Perfil de fisioterapeuta não encontrado."));
        if (!request.inicio().isBefore(request.fim()) || !request.inicio().isAfter(OffsetDateTime.now()))
            throw ErroNegocio.invalido("O bloqueio deve ser um período futuro válido.");
        if (consultas.existeConflito(fisioId, request.inicio(), request.fim()))
            throw ErroNegocio.conflito("Já existe uma consulta neste período.");
        if (bloqueios.existsByFisioterapeuta_Usuario_IdAndInicioLessThanAndFimGreaterThan(
            fisioId, request.fim(), request.inicio()))
            throw ErroNegocio.conflito("Este período já está bloqueado.");
        BloqueioAgenda bloqueio = new BloqueioAgenda();
        bloqueio.fisioterapeuta = fisio; bloqueio.inicio = request.inicio();
        bloqueio.fim = request.fim(); bloqueio.motivo = request.motivo();
        bloqueios.save(bloqueio);
    }

    @Transactional(readOnly = true)
    public List<ConsultaResumo> minhas(Long usuarioId, PerfilUsuario perfil) {
        List<Consulta> lista = switch (perfil) {
            case PACIENTE -> consultas.findByPaciente_Usuario_IdOrderByInicioDesc(usuarioId);
            case FISIO -> consultas.findByFisioterapeuta_Usuario_IdOrderByInicioDesc(usuarioId);
            default -> throw ErroNegocio.proibido("O perfil administrador não possui agenda pessoal.");
        };
        return lista.stream().map(this::resumir).toList();
    }

    @Transactional
    public ConsultaResumo cancelar(Long consultaId, Long usuarioId, PerfilUsuario perfil, String motivo) {
        Consulta consulta = consultas.bloquearPorId(consultaId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
        if (!consulta.reservaAtiva) throw ErroNegocio.conflito("Esta consulta já foi cancelada ou encerrada.");
        if (perfil == PerfilUsuario.PACIENTE) {
            if (!consulta.paciente.usuario.id.equals(usuarioId))
                throw ErroNegocio.proibido("Você só pode cancelar suas próprias consultas.");
            if (consulta.inicio.isBefore(OffsetDateTime.now().plusHours(24)))
                throw ErroNegocio.conflito("O cancelamento pelo paciente exige pelo menos 24 horas de antecedência.");
            consulta.status = StatusConsulta.CANCELADA_PACIENTE;
        } else if (perfil == PerfilUsuario.FISIO) {
            if (!consulta.fisioterapeuta.usuario.id.equals(usuarioId))
                throw ErroNegocio.proibido("Você só pode cancelar consultas da sua agenda.");
            consulta.status = StatusConsulta.CANCELADA_FISIO;
        } else if (perfil == PerfilUsuario.ADMIN) {
            consulta.status = StatusConsulta.CANCELADA_ADMIN;
        } else {
            throw ErroNegocio.proibido("Perfil sem permissão para cancelar.");
        }
        consulta.motivoCancelamento = motivo;
        consulta.reservaAtiva = false;
        return resumir(consulta);
    }

    @Transactional
    public ConsultaResumo confirmar(Long consultaId, Long fisioId) {
        Consulta consulta = consultas.bloquearPorId(consultaId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
        if (!consulta.fisioterapeuta.usuario.id.equals(fisioId))
            throw ErroNegocio.proibido("Esta consulta não pertence à sua agenda.");
        if (consulta.status != StatusConsulta.SOLICITADA)
            throw ErroNegocio.conflito("Somente solicitações pendentes podem ser confirmadas.");
        consulta.status = StatusConsulta.CONFIRMADA;
        return resumir(consulta);
    }

    @Transactional
    public ConsultaResumo concluir(Long consultaId, Long fisioId) {
        Consulta consulta = consultas.bloquearPorId(consultaId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
        if (!consulta.fisioterapeuta.usuario.id.equals(fisioId))
            throw ErroNegocio.proibido("Esta consulta não pertence à sua agenda.");
        if (consulta.status != StatusConsulta.CONFIRMADA)
            throw ErroNegocio.conflito("Somente consultas confirmadas podem ser encerradas.");
        consulta.status = StatusConsulta.REALIZADA;
        consulta.reservaAtiva = false;
        return resumir(consulta);
    }

    @Transactional
    public void avaliar(Long consultaId, Long pacienteId, AvaliacaoRequest request) {
        Consulta consulta = consultas.findById(consultaId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Consulta não encontrada."));
        if (!consulta.paciente.usuario.id.equals(pacienteId))
            throw ErroNegocio.proibido("Você só pode avaliar suas próprias consultas.");
        if (consulta.status != StatusConsulta.REALIZADA)
            throw ErroNegocio.conflito("A avaliação está disponível após a sessão.");
        if (avaliacoes.existsByConsulta_Id(consultaId))
            throw ErroNegocio.conflito("Esta consulta já recebeu uma avaliação.");
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.consulta = consulta;
        avaliacao.paciente = consulta.paciente;
        avaliacao.fisioterapeuta = consulta.fisioterapeuta;
        avaliacao.nota = request.nota();
        avaliacao.comentario = request.comentario();
        avaliacao.status = "PENDENTE";
        avaliacoes.save(avaliacao);
    }

    @Transactional
    public void moderarAvaliacao(Long avaliacaoId, String status) {
        Avaliacao avaliacao = avaliacoes.findById(avaliacaoId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Avaliação não encontrada."));
        avaliacao.status = status;
    }

    private boolean dentroDaDisponibilidade(Long fisioId, OffsetDateTime inicio, OffsetDateTime fim, int duracao) {
        ZonedDateTime localInicio = inicio.atZoneSameInstant(zoneId);
        ZonedDateTime localFim = fim.atZoneSameInstant(zoneId);
        if (!localInicio.toLocalDate().equals(localFim.toLocalDate())) return false;
        for (DisponibilidadeSemanal d : disponibilidades.findByFisioterapeuta_Usuario_IdAndDiaSemanaAndAtivaTrue(
            fisioId, localInicio.getDayOfWeek().getValue())) {
            LocalDate data = localInicio.toLocalDate();
            if (data.isBefore(d.vigenciaInicio) || (d.vigenciaFim != null && data.isAfter(d.vigenciaFim))) continue;
            long deslocamento = Duration.between(d.horaInicio, localInicio.toLocalTime()).toMinutes();
            if (!localInicio.toLocalTime().isBefore(d.horaInicio) &&
                !localFim.toLocalTime().isAfter(d.horaFim) && deslocamento >= 0 && deslocamento % duracao == 0)
                return true;
        }
        return false;
    }

    private ConsultaResumo resumir(Consulta c) {
        return new ConsultaResumo(c.id, c.paciente.usuario.id, c.paciente.usuario.nome,
            c.fisioterapeuta.usuario.id, c.fisioterapeuta.usuario.nome, c.servico.id,
            c.servico.nome, c.inicio, c.fim, c.status, c.preco);
    }
}
