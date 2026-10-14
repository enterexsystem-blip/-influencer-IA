# Ayla: Telegram + Instagram com memória compartilhada

## Objetivo
Adicionar Telegram ao backend existente da Ayla sem quebrar o atendimento do Instagram. As duas plataformas podem consultar o mesmo perfil de memória somente depois de um vínculo confirmado pelo usuário.

## Estrutura de identidade
- Guardar identidades de plataforma separadamente: `instagram:<id>` e `telegram:<id>`.
- Criar uma tabela de vínculos que associa essas identidades a um identificador interno aleatório.
- Nunca vincular contas somente por nome, @username, foto ou nome de exibição.
- Usar código de vínculo de uso único, com expiração curta, armazenado de forma segura e invalidado após o uso.
- Exigir confirmação explícita e explicar qual memória será compartilhada.
- Permitir desvincular contas e apagar memórias mediante comando autenticado.

## Segurança do webhook
- Usar HTTPS e o cabeçalho secreto oficial do webhook Telegram.
- Comparar o segredo no servidor antes de processar qualquer atualização.
- Não registrar tokens, códigos de vínculo nem conteúdo pessoal desnecessário nos logs.
- Limitar tamanho de mensagem, frequência de requisições e deduplicar updates.
- Guardar segredos somente nas variáveis privadas do serviço de hospedagem, nunca neste repositório.

## Memória
O histórico, preferências e fatos explícitos ficam no PostgreSQL existente. As mensagens devem ser atribuídas ao perfil interno correto antes de consultar ou atualizar a memória. O compartilhamento entre plataformas não deve juntar os perfis de outras pessoas que conversam com Ayla.

## Regra de tratamento
A saudação "senhor" é exclusiva do perfil de Kyodai. Não inferir essa identidade pelo nome de exibição; configurar a identidade autorizada no servidor usando identificadores verificados. Para outras pessoas, Ayla fala normalmente.

## Áudios e mídia
Os arquivos binários de áudio, foto e vídeo precisam ser enviados para armazenamento apropriado. O Git pode manter a organização e documentação, mas não deve ser usado para guardar tokens ou grandes coleções privadas de mídia. Cada áudio deve ter metadados de intenção, emoção, destinatário permitido e frase transcrita para que o bot não selecione uma gravação inadequada.

## Etapas
1. Criar o bot com BotFather e guardar o token como segredo.
2. Implementar endpoint Telegram no backend sem alterar o webhook do Instagram.
3. Criar vínculo temporário de uso único e confirmação nos dois lados.
4. Unificar o acesso ao perfil interno de memória no PostgreSQL.
5. Adicionar seleção de áudio e envio de fotos/vídeos.
6. Testar segurança, duplicação de eventos, isolamento de perfis e regressões do Instagram.
