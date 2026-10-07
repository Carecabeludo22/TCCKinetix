package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ProfissionalService {
    private final FisioterapeutaRepository fisioterapeutas;
    private final ServicoRepository servicos;
    private final AvaliacaoRepository avaliacoes;
    private final EspecialidadeRepository especialidades;
    private final PlanoAssinaturaRepository planos;
    private final DisponibilidadeRepository disponibilidades;

    public ProfissionalService(FisioterapeutaRepository fisioterapeutas, ServicoRepository servicos,
        AvaliacaoRepository avaliacoes, EspecialidadeRepository especialidades, PlanoAssinaturaRepository planos,
        DisponibilidadeRepository disponibilidades) {
        this.fisioterapeutas = fisioterapeutas;
        this.servicos = servicos;
        this.avaliacoes = avaliacoes;
        this.especialidades = especialidades;
        this.planos = planos;
        this.disponibilidades = disponibilidades;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResumo> buscar(String cidade, String bairro, String uf, Long especialidadeId,
        Double latitude, Double longitude, Double raioKm) {
        if ((latitude == null) != (longitude == null))
            throw ErroNegocio.invalido("Informe latitude e longitude juntas.");
        if (raioKm != null && (raioKm <= 0 || raioKm > 100))
            throw ErroNegocio.invalido("O raio de busca deve estar entre 0 e 100 km.");
        BigDecimal minLat = null, maxLat = null, minLon = null, maxLon = null;
        if (latitude != null) {
            double raio = raioKm == null ? 15 : raioKm;
            double deltaLat = raio / 111.0;
            double cos = Math.max(0.01, Math.abs(Math.cos(Math.toRadians(latitude))));
            double deltaLon = Math.min(180, raio / (111.0 * cos));
            minLat = BigDecimal.valueOf(Math.max(-90, latitude - deltaLat));
            maxLat = BigDecimal.valueOf(Math.min(90, latitude + deltaLat));
            minLon = BigDecimal.valueOf(Math.max(-180, longitude - deltaLon));
            maxLon = BigDecimal.valueOf(Math.min(180, longitude + deltaLon));
        }
        List<Fisioterapeuta> encontrados = fisioterapeutas.buscarPublicos(
            vazio(cidade), vazio(bairro), vazio(uf), especialidadeId, minLat, maxLat, minLon, maxLon, PageRequest.of(0, 100));
        return encontrados.stream().map(f -> resumir(f, latitude, longitude))
            .sorted(latitude == null ? Comparator.comparing(ProfissionalResumo::nome, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(ProfissionalResumo::distanciaKm, Comparator.nullsLast(Double::compareTo)))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalDetalhe perfil(Long id) {
        Fisioterapeuta fisio = fisioterapeutas.buscarPerfilPublico(id)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Profissional não encontrado."));
        ProfissionalResumo resumo = resumir(fisio, null, null);
        List<ServicoResumo> servicosAtivos = servicos.findByFisioterapeuta_Usuario_IdAndAtivoTrueOrderByNome(id)
            .stream().map(this::resumirServico).toList();
        List<AvaliacaoPublica> reviews = avaliacoes.findTop10ByFisioterapeuta_Usuario_IdAndStatusOrderByCriadoEmDesc(id, "APROVADA")
            .stream().map(a -> new AvaliacaoPublica(a.id, a.nota, a.comentario, a.criadoEm)).toList();
        return new ProfissionalDetalhe(resumo, servicosAtivos, reviews);
    }

    @Transactional(readOnly = true)
    public List<EspecialidadeResumo> listarEspecialidades() {
        return especialidades.findByAtivaTrueOrderByNome().stream()
            .map(e -> new EspecialidadeResumo(e.id, e.nome, e.descricao)).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanoAssinaturaResumo> listarPlanos() {
        return planos.findByAtivoTrueOrderByPrecoMensal().stream()
            .map(p -> new PlanoAssinaturaResumo(p.id, p.nome, p.descricao, p.precoMensal)).toList();
    }

    @Transactional
    public ServicoResumo criarServico(Long usuarioId, ServicoRequest request) {
        Fisioterapeuta fisio = meuPerfil(usuarioId);
        Servico servico = new Servico();
        servico.fisioterapeuta = fisio;
        preencherServico(servico, request);
        servicos.save(servico);
        return resumirServico(servico);
    }

    @Transactional(readOnly = true)
    public List<ServicoResumo> meusServicos(Long usuarioId) {
        meuPerfil(usuarioId);
        return servicos.findByFisioterapeuta_Usuario_IdAndAtivoTrueOrderByNome(usuarioId)
            .stream().map(this::resumirServico).toList();
    }

    @Transactional
    public ServicoResumo atualizarServico(Long usuarioId, Long servicoId, ServicoRequest request) {
        Servico servico = servicos.findByIdAndFisioterapeuta_Usuario_IdAndAtivoTrue(servicoId, usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço não encontrado."));
        preencherServico(servico, request);
        return resumirServico(servico);
    }

    @Transactional
    public void desativarServico(Long usuarioId, Long servicoId) {
        Servico servico = servicos.findByIdAndFisioterapeuta_Usuario_IdAndAtivoTrue(servicoId, usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço não encontrado."));
        servico.ativo = false;
    }

    @Transactional
    public void adicionarDisponibilidade(Long usuarioId, DisponibilidadeRequest request) {
        Fisioterapeuta fisio = meuPerfil(usuarioId);
        if (!request.horaInicio().isBefore(request.horaFim()))
            throw ErroNegocio.invalido("A hora final deve ser posterior à hora inicial.");
        if (request.vigenciaFim() != null && request.vigenciaInicio().isAfter(request.vigenciaFim()))
            throw ErroNegocio.invalido("A vigência final não pode anteceder a inicial.");
        boolean conflito = disponibilidades.existsByFisioterapeuta_Usuario_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFimGreaterThanAndAtivaTrue(
            usuarioId, request.diaSemana(), request.horaFim(), request.horaInicio());
        if (conflito) throw ErroNegocio.conflito("Este intervalo sobrepõe uma disponibilidade já cadastrada.");
        DisponibilidadeSemanal disponibilidade = new DisponibilidadeSemanal();
        disponibilidade.fisioterapeuta = fisio;
        disponibilidade.diaSemana = request.diaSemana();
        disponibilidade.horaInicio = request.horaInicio();
        disponibilidade.horaFim = request.horaFim();
        disponibilidade.vigenciaInicio = request.vigenciaInicio();
        disponibilidade.vigenciaFim = request.vigenciaFim();
        disponibilidades.save(disponibilidade);
    }

    @Transactional(readOnly = true)
    public ProfissionalResumo meuPerfilResumo(Long usuarioId) {
        Fisioterapeuta f = fisioterapeutas.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Perfil de fisioterapeuta não encontrado."));
        return resumir(f, null, null);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeSemanal> minhasDisponibilidades(Long usuarioId) {
        meuPerfil(usuarioId);
        return disponibilidades.findByFisioterapeuta_Usuario_IdAndAtivaTrueOrderByDiaSemanaAscHoraInicioAsc(usuarioId);
    }

    @Transactional
    public ProfissionalResumo atualizarPerfil(Long usuarioId, PerfilFisioRequest request) {
        Fisioterapeuta f = fisioterapeutas.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Perfil de fisioterapeuta não encontrado."));
        if (request.nomeClinica() != null) f.nomeClinica = request.nomeClinica();
        if (request.apresentacao() != null) f.apresentacao = request.apresentacao();
        if (request.logradouro() != null) f.logradouro = request.logradouro();
        if (request.numero() != null) f.numero = request.numero();
        if (request.complemento() != null) f.complemento = request.complemento();
        if (request.bairro() != null) f.bairro = request.bairro();
        if (request.cidade() != null) f.cidade = request.cidade();
        if (request.uf() != null) f.uf = request.uf().toUpperCase();
        if (request.cep() != null) f.cep = request.cep();
        if (request.latitude() != null) f.latitude = request.latitude();
        if (request.longitude() != null) f.longitude = request.longitude();
        if (request.atendeOnline() != null) f.atendeOnline = request.atendeOnline();
        if (request.fotoUrl() != null) f.usuario.fotoUrl = request.fotoUrl();
        return resumir(f, null, null);
    }

    @Transactional
    public void verificarCrefito(Long usuarioId, boolean verificado) {
        Fisioterapeuta f = fisioterapeutas.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Profissional não encontrado."));
        f.crefitoVerificado = verificado;
    }

    private Fisioterapeuta meuPerfil(Long usuarioId) {
        return fisioterapeutas.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> ErroNegocio.naoEncontrado("Perfil de fisioterapeuta não encontrado."));
    }

    private void preencherServico(Servico service, ServicoRequest request) {
        if (request.preco() == null || request.preco().signum() < 0)
            throw ErroNegocio.invalido("O preço deve ser informado e não pode ser negativo.");
        service.nome = request.nome().trim();
        service.descricao = request.descricao();
        service.duracaoMinutos = request.duracaoMinutos();
        service.preco = request.preco();
    }

    private ProfissionalResumo resumir(Fisioterapeuta f, Double latitude, Double longitude) {
        Double distancia = latitude == null ? null : distanciaKm(latitude, longitude,
            f.latitude == null ? null : f.latitude.doubleValue(), f.longitude == null ? null : f.longitude.doubleValue());
        Double media = avaliacoes.mediaPublica(f.usuario.id);
        return new ProfissionalResumo(f.usuario.id, f.usuario.nome, f.usuario.fotoUrl, f.nomeClinica,
            f.apresentacao, f.bairro, f.cidade, f.uf, f.logradouro, f.numero, f.complemento,
            f.latitude, f.longitude, f.crefitoUf, f.especialidades.stream().map(e -> e.nome).collect(java.util.stream.Collectors.toSet()),
            media == null ? 0 : media, avaliacoes.totalPublicas(f.usuario.id), distancia);
    }

    private ServicoResumo resumirServico(Servico s) {
        return new ServicoResumo(s.id, s.nome, s.descricao, s.duracaoMinutos, s.preco);
    }

    private String vazio(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private double distanciaKm(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Double.POSITIVE_INFINITY;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

}
