# Backend — Influencer IA

Backend do aplicativo Android. O endpoint `POST /chat` recebe mensagens do chat e das respostas automáticas, registra apenas sinais resumidos de estilo (gírias conhecidas, palavras recorrentes, abreviações e uso de emojis) e usa essa memória para personalizar respostas.

## Configuração

- Node.js 20+
- Chave de modelo em uma variável segura do servidor:
  - `GEMINI_API_KEY` para uma chave Google Gemini; ou
  - o mesmo campo pode conter uma chave OpenRouter iniciada por `sk-or-v1-`. O backend identifica esse formato e seleciona OpenRouter.
  - Opcionalmente, use `AI_API_KEY` ou `OPENROUTER_API_KEY`.
- `GEMINI_MODEL` é opcional; padrão Gemini: `gemini-2.5-flash-lite`.
- `AI_MODEL` é opcional; padrão OpenRouter: `openrouter/free`, que encaminha para um modelo gratuito disponível. A disponibilidade e os limites gratuitos podem variar.
- `DATABASE_URL` liga a memória persistente PostgreSQL. Sem ela, a memória fica apenas na RAM e some quando o processo reinicia.
- `PORT` é fornecida pelo host; localmente o padrão é `8080`.

Nunca coloque chaves de API em commits, no APK ou em arquivos públicos.

## Rotas

### `GET /health`

Retorna estado do serviço, provedor/modelo selecionado, se há uma chave configurada e se a memória está em PostgreSQL ou apenas na memória temporária do processo. Não expõe o segredo.

### `POST /chat`

Exemplo de corpo:

```json
{
  "message": "eae mn, suave?",
  "relationshipLevel": 0,
  "userId": "app:identificador-estavel"
}
```

O `userId` é obrigatório e deve ser estável para a mesma pessoa. O aplicativo usa um identificador local estável para o chat; respostas automáticas geram um identificador derivado de hash por aplicativo de mensagens e contato, evitando misturar estilos de pessoas diferentes.

A memória não guarda o texto completo das mensagens; guarda contagens limitadas de vocabulário e alguns sinais de estilo. A aprendizagem é gradual e baseada em expressões observadas, não é treinamento dos pesos do modelo. Sem uma chave válida, o backend usa um pequeno conjunto de respostas locais; esse modo é limitado e não equivale a uma IA generativa.

## Implantação e persistência

O `render.yaml` descreve um serviço web e um banco PostgreSQL para persistir perfis entre reinicializações. Se o banco ainda não existir no Render, sincronize o Blueprint e confira em `/health` que `memory` retorna `persistent-postgresql`. Se mostrar `temporary-process-memory`, a memória não está persistindo.

Antes de uso público, adicione autenticação, limites compartilhados por usuário, proteção contra abuso e controles para apagar a memória. Respostas automáticas só devem ser ativadas com consentimento explícito; revise os aplicativos permitidos e não permita respostas automáticas para códigos de acesso ou notificações sensíveis.


## Instagram Direct — respostas automáticas da Ayla

O backend agora inclui um webhook para receber mensagens de texto de DMs e responder usando a persona da Ayla e o mesmo mecanismo de IA/memória. A integração usa a API oficial do Instagram, não automação por senha, scraping ou simulação de toques.

### Pré-requisitos da Meta

1. Use uma conta profissional do Instagram (Business ou Creator) e configure o acesso à API do Instagram no Meta for Developers.
2. Configure a API com Instagram Login e conceda as permissões necessárias, incluindo `instagram_business_basic` e `instagram_business_manage_messages`.
3. Configure Webhooks para o objeto Instagram e assine o evento `messages`.
4. URL de callback: `https://influencer-ia-backend.onrender.com/instagram/webhook`
5. Token de verificação: use exatamente o valor definido em `INSTAGRAM_WEBHOOK_VERIFY_TOKEN`.
6. Configure as credenciais abaixo no painel do serviço Render, em Environment. Nunca as publique no repositório nem no APK.

### Variáveis de ambiente

- `INSTAGRAM_ACCOUNT_ID`: ID da conta profissional do Instagram.
- `INSTAGRAM_ACCESS_TOKEN`: token de acesso da conta com permissão para mensagens.
- `META_APP_SECRET`: App Secret do app Meta; usado para validar `X-Hub-Signature-256`.
- `INSTAGRAM_WEBHOOK_VERIFY_TOKEN`: segredo aleatório criado por você para validar a inscrição do webhook.
- `META_GRAPH_API_VERSION`: opcional; padrão atual do código `v26.0`.

Após salvar as variáveis, confirme em `/health` que `instagram.configured` aparece como `true`. A verificação de webhook no painel da Meta deve passar antes de testar uma DM.

### Limites e segurança

- A resposta automática trata apenas mensagens de texto recebidas; ignora eventos de eco e mensagens sem texto.
- O destinatário precisa iniciar a conversa. A API e as políticas da Meta limitam respostas automáticas à janela de atendimento permitida (normalmente 24 horas após a mensagem do usuário).
- O app pode exigir modo de desenvolvimento/testadores ou análise e aprovação da Meta para responder a pessoas fora das funções de teste.
- Os IDs de mensagens são deduplicados temporariamente em memória; para produção com alta disponibilidade, mova a deduplicação para PostgreSQL.
- Antes de ativar para público, teste com uma conta autorizada e monitore os logs do Render. Não envie mensagens promocionais não solicitadas.
- O webhook rejeita POSTs sem assinatura válida. A IA não deve alegar que é uma pessoa real; Ayla é uma personagem virtual.
