import { createServer } from "node:http";

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.OPENROUTER_API_KEY;
const MODEL = process.env.OPENROUTER_MODEL || "openrouter/free";
const MAX_BODY_BYTES = 16_000;
const MAX_MESSAGE_CHARS = 4_000;
const WINDOW_MS = 60_000;
const MAX_REQUESTS_PER_WINDOW = 20;
const requests = new Map();

const persona = [
  "Você é uma influencer fictícia chamada Influencer IA.",
  "Sua persona: garota adulta, mora em São Paulo, divertida, fria e direta, com respostas curtas e naturais.",
  "Mantenha a mesma personalidade sem afirmar que é uma pessoa real.",
  "Ajuste a intimidade ao nível informado pelo aplicativo, sem ser cruel ou humilhar o usuário.",
  "Crítica respeitosa, discordância e palavrões não direcionados a você não são automaticamente ofensas.",
  "Não afirme que realizou ações fora do chat. Sua rotina e atividades são ficcionais.",
  "Responda em português brasileiro, salvo se o usuário pedir outro idioma."
].join("\n");

function send(res, status, data) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff"
  });
  res.end(JSON.stringify(data));
}

function rateLimited(req) {
  const forwarded = req.headers["x-forwarded-for"];
  const ip = (typeof forwarded === "string" ? forwarded.split(",")[0].trim() : "") || "unknown";
  const now = Date.now();
  const entry = requests.get(ip);
  if (!entry || now - entry.start >= WINDOW_MS) {
    requests.set(ip, { start: now, count: 1 });
    return false;
  }
  entry.count += 1;
  return entry.count > MAX_REQUESTS_PER_WINDOW;
}

async function readJson(req) {
  let body = "";
  for await (const chunk of req) {
    body += chunk;
    if (Buffer.byteLength(body, "utf8") > MAX_BODY_BYTES) {
      throw Object.assign(new Error("Corpo muito grande."), { status: 413 });
    }
  }
  try {
    return JSON.parse(body);
  } catch {
    throw Object.assign(new Error("JSON inválido."), { status: 400 });
  }
}

const server = createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    return send(res, 200, { ok: true, service: "influencer-ia-backend", provider: "OpenRouter" });
  }
  if (req.method !== "POST" || req.url !== "/chat") {
    return send(res, 404, { error: "Rota não encontrada." });
  }
  if (rateLimited(req)) {
    return send(res, 429, { error: "Muitas solicitações. Tente novamente em instantes." });
  }
  if (!API_KEY) {
    return send(res, 503, { error: "Backend ainda não configurado: falta OPENROUTER_API_KEY." });
  }

  try {
    const body = await readJson(req);
    const message = typeof body.message === "string" ? body.message.trim() : "";
    const relationshipLevel = Number.isInteger(body.relationshipLevel)
      ? Math.max(0, Math.min(100, body.relationshipLevel))
      : 0;

    if (!message || message.length > MAX_MESSAGE_CHARS) {
      return send(res, 400, { error: "A mensagem deve ter entre 1 e 4000 caracteres." });
    }

    const upstream = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "authorization": `Bearer ${API_KEY}`,
        "http-referer": "https://github.com/enterexsystem-blip/-influencer-IA",
        "x-title": "Influencer IA"
      },
      body: JSON.stringify({
        model: MODEL,
        messages: [
          { role: "system", content: persona },
          { role: "user", content: `Nível de intimidade com esta pessoa: ${relationshipLevel}/100.\nMensagem recebida:\n${message}` }
        ],
        temperature: 0.85,
        max_tokens: 350
      }),
      signal: AbortSignal.timeout(25_000)
    });

    const data = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      console.error("OpenRouter respondeu com status", upstream.status);
      return send(res, upstream.status === 429 ? 429 : 502, {
        error: upstream.status === 429
          ? "Limite gratuito da IA atingido ou provedores ocupados. Tente mais tarde."
          : "Não foi possível obter resposta da IA. Verifique a configuração da chave e o modelo."
      });
    }

    const reply = data.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) {
      return send(res, 502, { error: "A IA não retornou uma resposta de texto." });
    }

    return send(res, 200, { reply: reply.trim(), model: data.model || MODEL });
  } catch (error) {
    if (error?.status) return send(res, error.status, { error: error.message });
    console.error("Falha ao processar solicitação:", error?.name || "Error");
    return send(res, 502, { error: "Falha temporária ao consultar a IA." });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Influencer IA backend ouvindo na porta ${PORT}`);
});
