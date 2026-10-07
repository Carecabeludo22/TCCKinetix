package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.NotificacaoResumo;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificacaoService {
    private final NotificacaoRepository notificacoes;
    private final DispositivoPushRepository dispositivos;
    private final UsuarioRepository usuarios;
    private final RestClient restClient;
    private final String expoUrl;

    public NotificacaoService(NotificacaoRepository notificacoes, DispositivoPushRepository dispositivos,
        UsuarioRepository usuarios, Environment environment) {
        this.notificacoes = notificacoes;
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
        this.restClient = RestClient.create();
        this.expoUrl = environment.getProperty("kinetix.push.expo-url", "https://exp.host/--/api/v2/push/send");
    }

    @Transactional
    public void agendarLembretes(Consulta consulta) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime lembrete24h = consulta.inicio.minusHours(24);
        OffsetDateTime enviar24h = lembrete24h.isBefore(agora) ? agora : lembrete24h;
        criar(consulta.paciente.usuario, consulta, "Lembrete de consulta", "Você tem uma consulta de fisioterapia agendada.", enviar24h);
        criar(consulta.fisioterapeuta.usuario, consulta, "Nova consulta", "Sua agenda tem uma consulta próxima.", agora);
        OffsetDateTime lembrete1h = consulta.inicio.minusHours(1);
        if (lembrete1h.isAfter(enviar24h))
            criar(consulta.paciente.usuario, consulta, "Lembrete de consulta", "Sua consulta de fisioterapia começa em breve.", lembrete1h);
    }

    @Transactional
    public void notificarPrescricao(PlanoExercicio plano) {
        criar(plano.paciente.usuario, null, "Nova rotina de exercícios",
            "Seu fisioterapeuta adicionou uma rotina de reabilitação.", OffsetDateTime.now());
    }

    @Transactional
    public NotificacaoResumo agendarLembreteExercicio(PlanoExercicio plano, OffsetDateTime enviarEm) {
        if (!enviarEm.isAfter(OffsetDateTime.now()))
            throw ErroNegocio.invalido("O lembrete precisa ser programado para um horário futuro.");
        Notificacao n = new Notificacao();
        n.usuario = plano.paciente.usuario;
        n.titulo = "Horário de exercício";
        n.mensagem = "Está na hora da sua rotina de reabilitação.";
        n.enviarEm = enviarEm;
        notificacoes.save(n);
        return new NotificacaoResumo(n.id,n.titulo,n.mensagem,n.enviarEm,n.status,n.enviadoEm);
    }

    @Transactional
    public DispositivoPush registrarDispositivo(Long usuarioId, String token, String plataforma) {
        Usuario usuario = usuarios.findById(usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Usuário não encontrado."));
        DispositivoPush dispositivo = dispositivos.findByToken(token).orElseGet(DispositivoPush::new);
        dispositivo.usuario = usuario;
        dispositivo.token = token;
        dispositivo.plataforma = plataforma;
        dispositivo.ativo = true;
        return dispositivos.save(dispositivo);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResumo> minhasNotificacoes(Long usuarioId) {
        return notificacoes.findByUsuario_IdOrderByCriadoEmDesc(usuarioId, PageRequest.of(0, 100))
            .stream().map(n -> new NotificacaoResumo(n.id, n.titulo, n.mensagem, n.enviarEm, n.status, n.enviadoEm)).toList();
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void enviarPendentes() {
        List<Notificacao> pendentes = notificacoes.findByStatusAndEnviarEmLessThanEqualOrderByEnviarEm(
            "PENDENTE", OffsetDateTime.now(), PageRequest.of(0, 50));
        for (Notificacao notificacao : pendentes) {
            List<DispositivoPush> ativos = dispositivos.findByUsuario_IdAndAtivoTrue(notificacao.usuario.id);
            if (ativos.isEmpty()) {
                notificacao.status = "SEM_DISPOSITIVO";
                continue;
            }
            try {
                for (DispositivoPush dispositivo : ativos) {
                    restClient.post().uri(expoUrl).body(Map.of(
                        "to", dispositivo.token,
                        "title", notificacao.titulo,
                        "body", notificacao.mensagem,
                        "sound", "default",
                        "data", Map.of("tipo", notificacao.consulta == null ? "PRESCRICAO" : "CONSULTA",
                            "consultaId", notificacao.consulta == null ? 0L : notificacao.consulta.id)
                    )).retrieve().toBodilessEntity();
                }
                notificacao.status = "ENVIADA";
                notificacao.enviadoEm = OffsetDateTime.now();
            } catch (Exception exception) {
                notificacao.status = "ERRO";
            }
        }
    }

    private void criar(Usuario usuario, Consulta consulta, String titulo, String mensagem, OffsetDateTime enviarEm) {
        Notificacao n = new Notificacao();
        n.usuario = usuario;
        n.consulta = consulta;
        n.titulo = titulo;
        n.mensagem = mensagem;
        n.enviarEm = enviarEm;
        notificacoes.save(n);
    }
}
