# Kinetix Mobile

Aplicativo React Native com Expo para pacientes: autenticação, busca pública por cidade, bairro, especialidade ou localização, visualização de profissionais e serviços, solicitação de horários, agenda, diário de dor, prescrições de exercícios e token de notificações push.

## Iniciar

1. Instale Node.js 22.13 ou superior e configure a API Java/SQL Server.
2. Copie `.env.example` para `.env` e ajuste `EXPO_PUBLIC_API_URL` para o endereço acessível pelo dispositivo (no Android Emulator, `10.0.2.2` aponta para o computador host).
3. Defina `EXPO_PUBLIC_EAS_PROJECT_ID` com o UUID do projeto EAS para registrar notificações push.
4. Execute `npm install` e `npx expo install --fix` para alinhar as dependências ao SDK Expo do projeto.
5. Execute `npx expo run:android` ou `npx expo run:ios` em um dispositivo/emulador compatível.

Push remoto requer credenciais de plataforma e development build configurados no EAS; Android Expo Go não recebe push remoto. Sem o EAS Project ID, login e agenda continuam funcionando, mas o app não registra token push.
