import test from "node:test";
import assert from "node:assert/strict";
import { readFile, readdir, stat } from "node:fs/promises";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const serverPath = join(root, "server.js");
const audioDir = join(root, "..", "media", "audios", "01-cumprimentos");

test("diagnostics endpoint and Telegram status command exist", async () => {
  const source = await readFile(serverPath, "utf8");
  assert.ok(source.includes('requestUrl.pathname === "/diagnostics"'));
  assert.ok(source.includes('text === "/status" || text === "/diagnostico"'));
  assert.ok(source.includes("runtimeDiagnostics.recentErrors"));
  assert.ok(source.includes("telegramWebhookLastError"));
});

test("audio response modes and text fallback exist", async () => {
  const source = await readFile(serverPath, "utf8");
  assert.ok(source.includes("__AYLA_BOTH__"));
  assert.ok(source.includes("__AYLA_AUDIO__"));
  assert.ok(source.includes("textFallbacks"));
  assert.ok(source.includes("runtimeDiagnostics.audioSent"));
  assert.ok(source.includes("async function chooseTelegramAudio"));
});

test("audio directory contains non-document files of plausible size", async () => {
  const entries = await readdir(audioDir, { withFileTypes: true });
  const files = entries.filter(entry => entry.isFile() && !/\.(zip|json|md|txt)$/i.test(entry.name));
  assert.ok(files.length > 0, "Nenhum arquivo de áudio encontrado");
  for (const file of files) {
    const info = await stat(join(audioDir, file.name));
    assert.ok(info.size > 1000, `Arquivo vazio ou pequeno demais: ${file.name}`);
  }
});

test("audio catalog is valid JSON with categories", async () => {
  const catalogPath = join(root, "..", "media", "audios", "catalog.json");
  const catalog = JSON.parse(await readFile(catalogPath, "utf8"));
  assert.ok(catalog.categories && typeof catalog.categories === "object");
  assert.ok(Object.keys(catalog.categories).length >= 4);
});
