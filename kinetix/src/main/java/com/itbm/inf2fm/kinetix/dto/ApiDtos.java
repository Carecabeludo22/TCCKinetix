package com.itbm.inf2fm.kinetix.dto;

import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.model.enums.StatusConsulta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}
    public record AuthResponse(Long usuarioId, String nome, PerfilUsuario perfil, String token, String tipoToken, OffsetDateTime expiraEm) {}
    public record PerfilPacienteRequest(@Size(max=120) String nome, @Size(max=30) String telefone,
        @Size(max=30) String documentoIdentidade, @Size(max=500) String fotoUrl, LocalDate dataNascimento,
        @Size(max=100) String cidade, @Size(max=100) String bairro, @Size(min=2,max=2) String uf) {}
    public record PacientePerfilResumo(Long usuarioId, String nome, String email, String telefone,
        String documentoIdentidade, String fotoUrl, LocalDate dataNascimento, String cidade, String bairro, String uf) {}
    public record LembreteExercicioRequest(@NotNull OffsetDateTime enviarEm) {}
    public record RegisterPatientRequest(
        @NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=100) String senha, @Size(max=30) String telefone,
        @Size(max=30) String documentoIdentidade, @Size(max=500) String fotoUrl,
        LocalDate dataNascimento, @Size(max=100) String cidade, @Size(max=100) String bairro, @Size(max=2) String uf) {}
    public record RegisterPhysioRequest(
        @NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=100) String senha, @Size(max=30) String telefone,
        @Size(max=30) String documentoIdentidade, @Size(max=500) String fotoUrl,
        @NotBlank @Size(max=30) String crefitoNumero, @NotBlank @Size(min=2,max=2) String crefitoUf,
        @Size(max=160) String nomeClinica, @Size(max=1200) String apresentacao,
        @NotBlank @Size(max=100) String cidade, @NotBlank @Size(min=2,max=2) String uf,
        @Size(max=160) String logradouro, @Size(max=20) String numero, @Size(max=100) String complemento,
        @Size(max=100) String bairro, @Size(max=9) String cep,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        boolean atendeOnline, Set<Long> especialidadesIds) {}
    public record ProfissionalResumo(Long id, String nome, String fotoUrl, String nomeClinica, String apresentacao,
        String bairro, String cidade, String uf, String logradouro, String numero, String complemento,
        BigDecimal latitude, BigDecimal longitude, String crefitoUf, Set<String> especialidades,
        Double avaliacaoMedia, long totalAvaliacoes, Double distanciaKm) {}
    public record AvaliacaoPublica(Long id, int nota, String comentario, java.time.OffsetDateTime criadoEm) {}
    public record ProfissionalDetalhe(ProfissionalResumo profissional, List<ServicoResumo> servicos,
        List<AvaliacaoPublica> avaliacoes) {}
    public record EspecialidadeRequest(@NotBlank @Size(max=100) String nome, @Size(max=500) String descricao) {}
    public record ExercicioRequest(@NotBlank @Size(max=120) String nome, @Size(max=1000) String descricao,
        @Size(max=500) String videoUrl) {}
    public record PlanoAssinaturaRequest(@NotBlank @Size(max=100) String nome, @Size(max=500) String descricao,
        @DecimalMin("0.00") BigDecimal precoMensal) {}
    public record AssinaturaResumo(Long id, Long planoId, String planoNome, String status,
        java.time.LocalDate inicio, java.time.LocalDate fim) {}
    public record EspecialidadeResumo(Long id, String nome, String descricao) {}
    public record VerificacaoRequest(boolean verificado) {}
    public record ServicoRequest(@NotBlank @Size(max=120) String nome, @Size(max=500) String descricao,
        @Min(10) @Max(480) int duracaoMinutos, @DecimalMin("0.00") BigDecimal preco) {}
    public record ServicoResumo(Long id, String nome, String descricao, int duracaoMinutos, BigDecimal preco) {}
    public record PerfilFisioRequest(@Size(max=160) String nomeClinica, @Size(max=1200) String apresentacao,
        @Size(max=160) String logradouro, @Size(max=20) String numero, @Size(max=100) String complemento,
        @Size(max=100) String bairro, @Size(max=100) String cidade, @Size(min=2,max=2) String uf,
        @Size(max=9) String cep, @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude, Boolean atendeOnline, @Size(max=500) String fotoUrl) {}
    public record DisponibilidadeRequest(@Min(1) @Max(7) int diaSemana, @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim, @NotNull LocalDate vigenciaInicio, LocalDate vigenciaFim) {}
    public record DisponibilidadeResumo(Long id, int diaSemana, java.time.LocalTime horaInicio,
        java.time.LocalTime horaFim, java.time.LocalDate vigenciaInicio, java.time.LocalDate vigenciaFim) {}
    public record BloqueioRequest(@NotNull OffsetDateTime inicio, @NotNull OffsetDateTime fim,
        @Size(max=300) String motivo) {}
    public record ConsultaRequest(@NotNull Long fisioterapeutaId, @NotNull Long servicoId, @NotNull OffsetDateTime inicio) {}
    public record ConsultaResumo(Long id, Long pacienteId, String pacienteNome, Long fisioterapeutaId,
        String fisioterapeutaNome, Long servicoId, String servicoNome, OffsetDateTime inicio,
        OffsetDateTime fim, StatusConsulta status, BigDecimal preco) {}
    public record CancelamentoRequest(@NotBlank @Size(max=500) String motivo) {}
    public record ProntuarioRequest(@NotBlank @Size(max=12000) String registroClinico) {}
    public record ProntuarioResumo(Long id, Long consultaId, Long pacienteId, Long fisioterapeutaId,
        String registroClinico, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {}
    public record RegistroDorRequest(@Min(0) @Max(10) int escalaEva, @Size(max=4000) String observacao,
        OffsetDateTime registradoEm) {}
    public record RegistroDorResumo(Long id, Long pacienteId, Long consultaId, int escalaEva,
        String observacao, OffsetDateTime registradoEm) {}
    public record ItemPrescricaoRequest(@NotNull Long exercicioId, @Min(1) int ordem,
        @Min(1) Integer series, @Min(1) Integer repeticoes, @Min(1) Integer duracaoSegundos,
        @Size(max=160) String frequencia, @Size(max=4000) String orientacoes) {}
    public record PrescricaoRequest(@NotBlank @Size(max=160) String titulo, @Size(max=8000) String orientacoes,
        @NotNull LocalDate inicio, LocalDate fim, Long consultaId, @NotEmpty List<@Valid ItemPrescricaoRequest> itens) {}
    public record ItemPrescricaoResumo(Long id, Long exercicioId, String exercicioNome, int ordem,
        Integer series, Integer repeticoes, Integer duracaoSegundos, String frequencia, String orientacoes) {}
    public record PrescricaoResumo(Long id, Long fisioterapeutaId, Long pacienteId, Long consultaId,
        String titulo, String orientacoes, LocalDate inicio, LocalDate fim, boolean ativo,
        List<ItemPrescricaoResumo> itens) {}
    public record AvaliacaoRequest(@Min(1) @Max(5) int nota, @Size(max=1000) String comentario) {}
    public record AvaliacaoModeracaoRequest(@NotBlank @Pattern(regexp="APROVADA|REJEITADA") String status) {}
    public record PlanoAssinaturaResumo(Long id, String nome, String descricao, BigDecimal precoMensal) {}
    public record PushTokenRequest(@NotBlank @Size(max=300) String token, @NotBlank @Pattern(regexp="IOS|ANDROID") String plataforma) {}
    public record NotificacaoResumo(Long id, String titulo, String mensagem, OffsetDateTime enviarEm,
        String status, OffsetDateTime enviadoEm) {}
    public record ErroResumo(OffsetDateTime instante, int status, String erro, String mensagem) {}
}
