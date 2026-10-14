import test from "node:test";
import assert from "node:assert/strict";
import { readFile, readdir, stat } from "node:fs/promises";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const serverPath = join(root, "server.js");
const audioDir = join(root, "..", "media", "audios", "01-cumprimentos");

test("backend has a detailed diagnostics endpoint and Telegram status command", async () => {
  const source = await readFile(serverPath, "utf8");
  assert.match(source, /requestUrl\.pathname === "\/diagnostics"/);
  assert.match(source, /text === "\/status" \|\| text === "\/diagnostico"/);
  assert.match(source, /runtimeDiagnostics\.recentErrors/);
  assert.match(source, /telegramWebhookLastError/);
});

test("audio mode has a text fallback and both mode can send text plus audio", async () => {
  const source = await readFile(serverPath, "utf8");
  assert.match(source, /__AYLA_BOTH__/);
  assert.match(source, /__AYLA_AUDIO__/);
  assert.match(source, /textFallbacks\+\+/);
  assert.match(source, /runtimeDiagnostics\.audioSent\+\+/);
  assert.match(source, /async function chooseTelegramAudio/);
});

test("audio directory exists and contains usable non-document files", async () => {
  const entries = await readdir(audioDir, { withFileTypes: true });
  const files = entries.filter(entry => entry.isFile() && !/\.(zip|json|md|txt)$/i.test(entry.name));
  assert.ok(files.length > 0, "Nenhum arquivo de áudio foi encontrado em media/audios/01-cumprimentos");
  for (const file of files) {
    const info = await stat(join(audioDir, file.name));
    assert.ok(info.size > 1000, `Arquivo vazio ou pequeno demais: ${file.name}`);
  }
});

test("catalog is valid JSON and references audio categories", async () => {
  const catalogPath = join(root, "..", "media", "audios", "catalog.json");
  const catalog = JSON.parse(await readFile(catalogPath, "utf8"));
  assert.ok(catalog.categories && typeof catalog.categories === "object");
  assert.ok(Object.keys(catalog.categories).length >= 4);
});
