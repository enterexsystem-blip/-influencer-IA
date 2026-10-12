import { createServer } from "node:http";

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.GEMINI_API_KEY;
const MODEL = process.env.GEMINI_MODEL || "gemini-2.5-flash-lite";
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
    return send(res, 200, {
      ok: true,
      service: "influencer-ia-backend",
      provider: "Google Gemini",
      model: MODEL,
      configured: Boolean(API_KEY)
    });
  }

  if (req.method !== "POST" || req.url !== "/chat") {
    return send(res, 404, { error: "Rota não encontrada." });
  }

  if (rateLimited(req)) {
    return send(res, 429, { error: "Muitas solicitações. Tente novamente em instantes." });
  }

  if (!API_KEY) {
    return send(res, 503, { error: "Backend ainda não configurado: falta GEMINI_API_KEY." });
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

    const upstream = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(MODEL)}:generateContent`,
      {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-goog-api-key": API_KEY
        },
        body: JSON.stringify({
          systemInstruction: {
            parts: [{ text: persona }]
          },
          contents: [{
            role: "user",
            parts: [{
              text: `Nível de intimidade com esta pessoa: ${relationshipLevel}/100.\nMensagem recebida:\n${message}`
            }]
          }],
          generationConfig: {
            temperature: 0.85,
            maxOutputTokens: 350
          }
        }),
        signal: AbortSignal.timeout(25_000)
      }
    );

    const data = await upstream.json().catch(() => ({}));

    if (!upstream.ok) {
      console.error("Gemini respondeu com status", upstream.status);
      if (upstream.status === 429) {
        return send(res, 429, {
          error: "Limite de solicitações do Gemini atingido. Aguarde e tente novamente."
        });
      }
      if (upstream.status === 400 || upstream.status === 403) {
        return send(res, 502, {
          error: "O Gemini recusou a solicitação. Confira a chave, o acesso à API e o modelo configurado."
        });
      }
      if (upstream.status === 401) {
        return send(res, 502, {
          error: "A chave do Gemini foi rejeitada. Confira GEMINI_API_KEY nas variáveis do servidor."
        });
      }
      return send(res, 502, { error: "Não foi possível obter resposta do Gemini neste momento." });
    }

    const reply = data.candidates?.[0]?.content?.parts
      ?.map(part => typeof part.text === "string" ? part.text : "")
      .join("")
      .trim();

    if (!reply) {
      const reason = data.promptFeedback?.blockReason;
      return send(res, 502, {
        error: reason
          ? "O Gemini bloqueou esta mensagem. Tente reformular o texto."
          : "O Gemini não retornou uma resposta de texto."
      });
    }

    return send(res, 200, { reply, model: MODEL, provider: "Google Gemini" });
  } catch (error) {
    if (error?.status) return send(res, error.status, { error: error.message });
    console.error("Falha ao processar solicitação:", error?.name || "Error");
    return send(res, 502, { error: "Falha temporária ao consultar o Gemini." });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Influencer IA backend ouvindo na porta ${PORT}; provedor Google Gemini; modelo ${MODEL}`);
});
