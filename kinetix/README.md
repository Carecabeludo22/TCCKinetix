# Kinetix API

Plataforma de atendimento fisioterapêutico com API REST Java, cliente web React/Vite e aplicativo React Native/Expo.

## Requisitos

- Java 21
- SQL Server
- Maven (ou mvnw.cmd)
- Node.js 22.13 ou superior para web/mobile

## Preparar o banco

1. Execute o script src/docs/database/kinetix.sql no SQL Server Management Studio para criar o banco vazio Kinetix.
2. Configure as variáveis de ambiente descritas abaixo.
3. Inicie a aplicação. O Flyway cria as tabelas e os índices a partir de src/main/resources/db/migration.

O SQL completo para execução manual também está em src/docs/database/Kinetix_SQL_Server.sql. Use apenas uma estratégia: no fluxo recomendado da aplicação, execute o bootstrap vazio e deixe o Flyway aplicar as migrations; o script completo manual não cria histórico Flyway.

A conta SQL Server precisa ter permissão para criar tabelas e índices em Kinetix.

## Variáveis de ambiente

- KINETIX_DB_URL: por padrão, jdbc:sqlserver://localhost:1433;databaseName=Kinetix;encrypt=true;trustServerCertificate=true
- KINETIX_DB_USERNAME e KINETIX_DB_PASSWORD: credenciais do SQL Server.
- KINETIX_JWT_SECRET_BASE64: chave aleatória Base64 com pelo menos 32 bytes para assinatura HS256.
- KINETIX_DATA_ENCRYPTION_KEY: chave AES-256 Base64 para criptografar dados clínicos.
- KINETIX_TIME_ZONE: fuso das agendas; padrão America/Sao_Paulo.
- KINETIX_WEB_ORIGINS: origens web permitidas por CORS, separadas por vírgula.

No PowerShell, gere duas chaves independentes usando RandomNumberGenerator.Fill para 32 bytes e converta cada array com Convert.ToBase64String.

Não reutilize essas chaves e não as versione. A perda da chave de criptografia impede a leitura dos registros clínicos. Em produção, configure HTTPS/TLS no balanceador ou proxy reverso e injete os segredos por um gerenciador de segredos.

## Executar

Defina KINETIX_DB_USERNAME e KINETIX_DB_PASSWORD no PowerShell e rode .\mvnw.cmd spring-boot:run.

O cliente web fica em `web/` e o app móvel em `mobile/`; cada pasta tem seu próprio README e arquivo `.env.example`.

## Principais rotas

- POST /api/v1/auth/pacientes, POST /api/v1/auth/fisioterapeutas, POST /api/v1/auth/login
- GET /api/v1/profissionais e GET /api/v1/profissionais/{id} (busca pública; somente CREFITO verificado)
- GET /api/v1/profissionais/{id}/horarios
- POST /api/v1/consultas, GET /api/v1/consultas/minhas, PATCH /api/v1/consultas/{id}/cancelar
- Gestão autenticada de serviços e disponibilidades em /api/v1/fisioterapeuta
- Prontuário, prescrições de exercícios e diário de dor em /api/v1

O login devolve um JWT Bearer. A API persiste tokens Expo e despacha lembretes de consulta em segundo plano; o aplicativo deve registrar o token em POST /api/v1/dispositivos-push. Busca pública não exige conta. Operações de saúde exigem token e validação de titularidade. O cadastro público não cria administradores; um DBA deve promover a primeira conta confiável para o perfil ADMIN na tabela dbo.usuarios para habilitar a verificação CREFITO e a moderação dos catálogos e avaliações.

Os registros clínicos, orientações de prescrições e observações de dor são criptografados com AES-GCM na aplicação. A escala de dor é validada entre 0 e 10. O agendamento serializa alterações por fisioterapeuta e ainda possui índice único para horários idênticos. O paciente só pode cancelar com pelo menos 24 horas de antecedência.
