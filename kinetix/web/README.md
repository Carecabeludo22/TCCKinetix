# Kinetix Web

Cliente React e Vite para pesquisa pública por cidade, bairro, especialidade ou localização, cadastro de paciente/fisioterapeuta, solicitação de consulta e gestão da agenda. O painel do fisioterapeuta permite manter o perfil da clínica, coordenadas, serviços e disponibilidades; pacientes atualizam o perfil e acompanham as consultas.

## Iniciar

1. Instale Node.js 22.13 ou superior.
2. Copie `.env.example` para `.env` e ajuste `VITE_API_URL` se a API não estiver em `http://localhost:8080/api/v1`.
3. Na pasta `web`, rode `npm install` e `npm run dev`.

O navegador deve acessar a API Java em execução; ajuste `KINETIX_WEB_ORIGINS` no backend se o endereço local do Vite não estiver liberado pelo CORS.
