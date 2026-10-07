CREATE TABLE dbo.usuarios (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuarios PRIMARY KEY,
    nome NVARCHAR(120) NOT NULL,
    email NVARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    telefone NVARCHAR(30) NULL,
    documento_identidade NVARCHAR(30) NULL,
    foto_url NVARCHAR(500) NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_usuarios_ativo DEFAULT 1,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_usuarios_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('PACIENTE', 'FISIO', 'ADMIN'))
);

CREATE TABLE dbo.pacientes (
    usuario_id BIGINT NOT NULL CONSTRAINT pk_pacientes PRIMARY KEY,
    data_nascimento DATE NULL,
    cidade NVARCHAR(100) NULL,
    bairro NVARCHAR(100) NULL,
    uf CHAR(2) NULL,
    CONSTRAINT fk_pacientes_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id)
);

CREATE TABLE dbo.fisioterapeutas (
    usuario_id BIGINT NOT NULL CONSTRAINT pk_fisioterapeutas PRIMARY KEY,
    crefito_numero NVARCHAR(30) NOT NULL,
    crefito_uf CHAR(2) NOT NULL,
    crefito_verificado BIT NOT NULL CONSTRAINT df_fisioterapeutas_verificado DEFAULT 0,
    nome_clinica NVARCHAR(160) NULL,
    apresentacao NVARCHAR(1200) NULL,
    logradouro NVARCHAR(160) NULL,
    numero NVARCHAR(20) NULL,
    complemento NVARCHAR(100) NULL,
    bairro NVARCHAR(100) NULL,
    cidade NVARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,
    cep CHAR(9) NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    atende_online BIT NOT NULL CONSTRAINT df_fisioterapeutas_online DEFAULT 0,
    ativo BIT NOT NULL CONSTRAINT df_fisioterapeutas_ativo DEFAULT 1,
    CONSTRAINT fk_fisioterapeutas_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id),
    CONSTRAINT uq_fisioterapeutas_crefito UNIQUE (crefito_uf, crefito_numero),
    CONSTRAINT ck_fisioterapeutas_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_fisioterapeutas_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE dbo.especialidades (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_especialidades PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL CONSTRAINT uq_especialidades_nome UNIQUE,
    descricao NVARCHAR(500) NULL,
    ativa BIT NOT NULL CONSTRAINT df_especialidades_ativa DEFAULT 1
);

CREATE TABLE dbo.fisioterapeuta_especialidades (
    fisioterapeuta_id BIGINT NOT NULL,
    especialidade_id BIGINT NOT NULL,
    CONSTRAINT pk_fisioterapeuta_especialidades PRIMARY KEY (fisioterapeuta_id, especialidade_id),
    CONSTRAINT fk_fisio_especialidades_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT fk_fisio_especialidades_especialidade FOREIGN KEY (especialidade_id) REFERENCES dbo.especialidades(id)
);

CREATE TABLE dbo.servicos (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_servicos PRIMARY KEY,
    fisioterapeuta_id BIGINT NOT NULL,
    nome NVARCHAR(120) NOT NULL,
    descricao NVARCHAR(500) NULL,
    duracao_minutos SMALLINT NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_servicos_ativo DEFAULT 1,
    CONSTRAINT fk_servicos_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT ck_servicos_duracao CHECK (duracao_minutos BETWEEN 10 AND 480),
    CONSTRAINT ck_servicos_preco CHECK (preco >= 0)
);

CREATE TABLE dbo.disponibilidades_semanais (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_disponibilidades PRIMARY KEY,
    fisioterapeuta_id BIGINT NOT NULL,
    dia_semana TINYINT NOT NULL,
    hora_inicio TIME(0) NOT NULL,
    hora_fim TIME(0) NOT NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE NULL,
    ativa BIT NOT NULL CONSTRAINT df_disponibilidades_ativa DEFAULT 1,
    CONSTRAINT fk_disponibilidades_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT ck_disponibilidades_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_disponibilidades_horas CHECK (hora_inicio < hora_fim),
    CONSTRAINT ck_disponibilidades_vigencia CHECK (vigencia_fim IS NULL OR vigencia_inicio <= vigencia_fim)
);

CREATE TABLE dbo.bloqueios_agenda (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_bloqueios_agenda PRIMARY KEY,
    fisioterapeuta_id BIGINT NOT NULL,
    inicio DATETIMEOFFSET(0) NOT NULL,
    fim DATETIMEOFFSET(0) NOT NULL,
    motivo NVARCHAR(300) NULL,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_bloqueios_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_bloqueios_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT ck_bloqueios_periodo CHECK (inicio < fim)
);

CREATE TABLE dbo.consultas (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_consultas PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    fisioterapeuta_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    inicio DATETIMEOFFSET(0) NOT NULL,
    fim DATETIMEOFFSET(0) NOT NULL,
    status VARCHAR(24) NOT NULL CONSTRAINT df_consultas_status DEFAULT 'SOLICITADA',
    reserva_ativa BIT NOT NULL CONSTRAINT df_consultas_reserva DEFAULT 1,
    preco DECIMAL(10,2) NOT NULL,
    duracao_minutos SMALLINT NOT NULL,
    motivo_cancelamento NVARCHAR(500) NULL,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_consultas_criado DEFAULT SYSDATETIMEOFFSET(),
    atualizado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_consultas_atualizado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_consultas_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id),
    CONSTRAINT fk_consultas_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT fk_consultas_servico FOREIGN KEY (servico_id) REFERENCES dbo.servicos(id),
    CONSTRAINT ck_consultas_periodo CHECK (inicio < fim),
    CONSTRAINT ck_consultas_preco CHECK (preco >= 0),
    CONSTRAINT ck_consultas_status CHECK (status IN ('SOLICITADA', 'CONFIRMADA', 'CANCELADA_PACIENTE', 'CANCELADA_FISIO', 'CANCELADA_ADMIN', 'REALIZADA', 'FALTOU'))
);

CREATE TABLE dbo.prontuarios (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_prontuarios PRIMARY KEY,
    consulta_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    fisioterapeuta_id BIGINT NOT NULL,
    registro_clinico_enc NVARCHAR(MAX) NOT NULL,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_prontuarios_criado DEFAULT SYSDATETIMEOFFSET(),
    atualizado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_prontuarios_atualizado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT uq_prontuarios_consulta UNIQUE (consulta_id),
    CONSTRAINT fk_prontuarios_consulta FOREIGN KEY (consulta_id) REFERENCES dbo.consultas(id),
    CONSTRAINT fk_prontuarios_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id),
    CONSTRAINT fk_prontuarios_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id)
);

CREATE TABLE dbo.exercicios (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_exercicios PRIMARY KEY,
    nome NVARCHAR(120) NOT NULL,
    descricao NVARCHAR(1000) NULL,
    video_url NVARCHAR(500) NULL,
    ativo BIT NOT NULL CONSTRAINT df_exercicios_ativo DEFAULT 1
);

CREATE TABLE dbo.planos_exercicios (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_planos_exercicios PRIMARY KEY,
    fisioterapeuta_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    consulta_id BIGINT NULL,
    titulo NVARCHAR(160) NOT NULL,
    orientacoes_enc NVARCHAR(MAX) NULL,
    inicio DATE NOT NULL,
    fim DATE NULL,
    ativo BIT NOT NULL CONSTRAINT df_planos_exercicios_ativo DEFAULT 1,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_planos_exercicios_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_planos_exercicios_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT fk_planos_exercicios_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id),
    CONSTRAINT fk_planos_exercicios_consulta FOREIGN KEY (consulta_id) REFERENCES dbo.consultas(id),
    CONSTRAINT ck_planos_exercicios_vigencia CHECK (fim IS NULL OR inicio <= fim)
);

CREATE TABLE dbo.itens_prescricao (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_itens_prescricao PRIMARY KEY,
    plano_id BIGINT NOT NULL,
    exercicio_id BIGINT NOT NULL,
    ordem SMALLINT NOT NULL,
    series SMALLINT NULL,
    repeticoes SMALLINT NULL,
    duracao_segundos INT NULL,
    frequencia NVARCHAR(160) NULL,
    orientacoes_enc NVARCHAR(MAX) NULL,
    CONSTRAINT fk_itens_prescricao_plano FOREIGN KEY (plano_id) REFERENCES dbo.planos_exercicios(id),
    CONSTRAINT fk_itens_prescricao_exercicio FOREIGN KEY (exercicio_id) REFERENCES dbo.exercicios(id),
    CONSTRAINT ck_itens_prescricao_ordem CHECK (ordem > 0),
    CONSTRAINT ck_itens_prescricao_medidas CHECK ((series IS NULL OR series > 0) AND (repeticoes IS NULL OR repeticoes > 0) AND (duracao_segundos IS NULL OR duracao_segundos > 0))
);

CREATE TABLE dbo.registros_dor (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_registros_dor PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    consulta_id BIGINT NULL,
    escala_eva TINYINT NOT NULL,
    observacao_enc NVARCHAR(MAX) NULL,
    registrado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_registros_dor_registrado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_registros_dor_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id),
    CONSTRAINT fk_registros_dor_consulta FOREIGN KEY (consulta_id) REFERENCES dbo.consultas(id),
    CONSTRAINT ck_registros_dor_eva CHECK (escala_eva BETWEEN 0 AND 10)
);

CREATE TABLE dbo.avaliacoes (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_avaliacoes PRIMARY KEY,
    consulta_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    fisioterapeuta_id BIGINT NOT NULL,
    nota TINYINT NOT NULL,
    comentario NVARCHAR(1000) NULL,
    status VARCHAR(16) NOT NULL CONSTRAINT df_avaliacoes_status DEFAULT 'PENDENTE',
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_avaliacoes_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT uq_avaliacoes_consulta UNIQUE (consulta_id),
    CONSTRAINT fk_avaliacoes_consulta FOREIGN KEY (consulta_id) REFERENCES dbo.consultas(id),
    CONSTRAINT fk_avaliacoes_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id),
    CONSTRAINT fk_avaliacoes_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT ck_avaliacoes_nota CHECK (nota BETWEEN 1 AND 5),
    CONSTRAINT ck_avaliacoes_status CHECK (status IN ('PENDENTE', 'APROVADA', 'REJEITADA'))
);

CREATE TABLE dbo.planos_assinatura (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_planos_assinatura PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL,
    descricao NVARCHAR(500) NULL,
    preco_mensal DECIMAL(10,2) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_planos_assinatura_ativo DEFAULT 1,
    CONSTRAINT ck_planos_assinatura_preco CHECK (preco_mensal >= 0)
);

CREATE TABLE dbo.assinaturas (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_assinaturas PRIMARY KEY,
    fisioterapeuta_id BIGINT NOT NULL,
    plano_id BIGINT NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_assinaturas_fisio FOREIGN KEY (fisioterapeuta_id) REFERENCES dbo.fisioterapeutas(usuario_id),
    CONSTRAINT fk_assinaturas_plano FOREIGN KEY (plano_id) REFERENCES dbo.planos_assinatura(id),
    CONSTRAINT ck_assinaturas_status CHECK (status IN ('ATIVA', 'CANCELADA', 'EXPIRADA', 'PENDENTE'))
);

CREATE TABLE dbo.dispositivos_push (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_dispositivos_push PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token NVARCHAR(300) NOT NULL,
    plataforma VARCHAR(16) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_dispositivos_push_ativo DEFAULT 1,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_dispositivos_push_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_dispositivos_push_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id),
    CONSTRAINT uq_dispositivos_push_token UNIQUE (token),
    CONSTRAINT ck_dispositivos_push_plataforma CHECK (plataforma IN ('IOS', 'ANDROID'))
);

CREATE TABLE dbo.notificacoes (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_notificacoes PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    consulta_id BIGINT NULL,
    titulo NVARCHAR(120) NOT NULL,
    mensagem NVARCHAR(300) NOT NULL,
    enviar_em DATETIMEOFFSET(0) NOT NULL,
    status VARCHAR(16) NOT NULL CONSTRAINT df_notificacoes_status DEFAULT 'PENDENTE',
    enviado_em DATETIMEOFFSET(0) NULL,
    criado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_notificacoes_criado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_notificacoes_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id),
    CONSTRAINT fk_notificacoes_consulta FOREIGN KEY (consulta_id) REFERENCES dbo.consultas(id),
    CONSTRAINT ck_notificacoes_status CHECK (status IN ('PENDENTE', 'ENVIADA', 'SEM_DISPOSITIVO', 'ERRO'))
);

CREATE TABLE dbo.auditoria_acesso_saude (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_auditoria_acesso_saude PRIMARY KEY,
    ator_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    recurso VARCHAR(40) NOT NULL,
    recurso_id BIGINT NULL,
    acao VARCHAR(20) NOT NULL,
    acessado_em DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_auditoria_acesso_saude_acessado DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT fk_auditoria_ator FOREIGN KEY (ator_id) REFERENCES dbo.usuarios(id),
    CONSTRAINT fk_auditoria_paciente FOREIGN KEY (paciente_id) REFERENCES dbo.pacientes(usuario_id)
);

INSERT INTO dbo.especialidades (nome, descricao) VALUES
    (N'Ortopedia e Traumatologia', N'Reabilitação de condições musculoesqueléticas.'),
    (N'Fisioterapia Esportiva', N'Prevenção e reabilitação de lesões relacionadas ao esporte.'),
    (N'Neurofuncional', N'Reabilitação de alterações neurológicas e motoras.'),
    (N'Respiratória', N'Fisioterapia voltada à função respiratória.'),
    (N'Geriátrica', N'Fisioterapia voltada à mobilidade e funcionalidade da pessoa idosa.'),
    (N'Pélvica', N'Fisioterapia da saúde pélvica.');

CREATE INDEX ix_fisioterapeutas_busca_local ON dbo.fisioterapeutas (uf, cidade, bairro, crefito_verificado, ativo) INCLUDE (latitude, longitude);
CREATE INDEX ix_fisio_especialidades_especialidade ON dbo.fisioterapeuta_especialidades (especialidade_id, fisioterapeuta_id);
CREATE INDEX ix_servicos_fisio_ativo ON dbo.servicos (fisioterapeuta_id, ativo);
CREATE INDEX ix_disponibilidades_fisio_dia ON dbo.disponibilidades_semanais (fisioterapeuta_id, dia_semana, ativa);
CREATE INDEX ix_bloqueios_fisio_periodo ON dbo.bloqueios_agenda (fisioterapeuta_id, inicio, fim);
CREATE INDEX ix_consultas_fisio_periodo ON dbo.consultas (fisioterapeuta_id, inicio, fim, status);
CREATE INDEX ix_consultas_paciente_periodo ON dbo.consultas (paciente_id, inicio DESC);
CREATE UNIQUE INDEX ux_consultas_reserva_horario ON dbo.consultas (fisioterapeuta_id, inicio) WHERE reserva_ativa = 1;
CREATE INDEX ix_prontuarios_paciente_data ON dbo.prontuarios (paciente_id, criado_em DESC);
CREATE INDEX ix_planos_exercicios_paciente_ativo ON dbo.planos_exercicios (paciente_id, ativo, inicio DESC);
CREATE INDEX ix_registros_dor_paciente_data ON dbo.registros_dor (paciente_id, registrado_em DESC);
CREATE INDEX ix_avaliacoes_fisio_publicas ON dbo.avaliacoes (fisioterapeuta_id, status, criado_em DESC);
CREATE INDEX ix_notificacoes_pendentes ON dbo.notificacoes (status, enviar_em) INCLUDE (usuario_id, consulta_id);
CREATE INDEX ix_auditoria_saude_paciente_data ON dbo.auditoria_acesso_saude (paciente_id, acessado_em DESC);
