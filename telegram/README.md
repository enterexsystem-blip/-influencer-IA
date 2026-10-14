# Ayla no Telegram

Este diretório é a integração Telegram planejada para o projeto existente da Ayla. O backend atual do Instagram deve continuar funcionando; a integração Telegram será adicionada sem substituir o webhook do Instagram.

## Regras de personalidade
- O mesmo perfil vinculado compartilha memória entre Instagram e Telegram após confirmação explícita.
- Somente o perfil principal autorizado (Kyodai) recebe o tratamento "senhor".
- Todas as outras pessoas são tratadas normalmente, com personalização baseada no próprio perfil.
- Não misturar usuários por nome de exibição ou @username; usar IDs estáveis da plataforma e uma tabela de vínculos validada no servidor.

## Segurança
- Guardar o token do bot apenas como variável de ambiente `TELEGRAM_BOT_TOKEN` no serviço de hospedagem.
- Proteger o webhook com `TELEGRAM_WEBHOOK_SECRET` e validar o cabeçalho `X-Telegram-Bot-Api-Secret-Token`.
- Não enviar tokens, códigos de vínculo ou segredos para o Git.
- Não ativar compartilhamento de memória sem confirmar os dois lados do vínculo.

## Estado
Estrutura inicial criada. A lógica do bot, o vínculo seguro e o envio de mídias serão implementados e testados antes de considerar a integração pronta.