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
