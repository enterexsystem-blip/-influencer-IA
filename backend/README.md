# Backend — Influencer IA + Gemini

Este serviço é um primeiro endpoint de chat para testar a persona com a API Gemini. Ele **não está implantado** e ainda não está conectado ao aplicativo Android.

## Requisitos
- Node.js 20 ou superior
- Uma chave criada no [Google AI Studio](https://aistudio.google.com/apikey)

## Executar localmente

Configure a chave como variável de ambiente, sem colocá-la em arquivos versionados:

```bash
export GEMINI_API_KEY="SUA_CHAVE"
npm start
```

Teste a saúde do serviço:

```bash
curl http://localhost:8080/health
```

Teste uma conversa:

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Oi, quem é você?","relationshipLevel":0}'
```

## Variáveis de ambiente
- `GEMINI_API_KEY` — obrigatória; segredo da API, nunca colocar no Git.
- `GEMINI_MODEL` — opcional; padrão `gemini-2.5-flash-lite`.
- `PORT` — opcional; padrão `8080`.

## Importante antes de publicar
- Este endpoint é um protótipo: limite de requisições é local e reinicia ao reiniciar o processo.
- Antes de uso público, adicionar autenticação, rate limiting compartilhado, quotas por usuário, monitoramento de custos/limites e proteção contra abuso.
- O endpoint não persiste histórico nem memória de usuário ainda.
- Hospedar o backend em HTTPS e guardar `GEMINI_API_KEY` no gerenciador de segredos do host.
- Não embutir a chave no APK, no código Android, no workflow do GitHub nem em commits.
- O nível de acesso gratuito tem limites e pode mudar; confira os preços e limites oficiais do Gemini API.
- Solicitações enviadas ao nível gratuito podem ser usadas para melhorar produtos do Google, conforme as condições oficiais.
