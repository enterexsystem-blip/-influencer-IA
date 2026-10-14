import { spawnSync } from "node:child_process";
import { readFile, readdir, stat, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const backendDir = dirname(dirname(fileURLToPath(import.meta.url)));
const rootDir = dirname(backendDir);
const audioDir = join(rootDir, "media", "audios", "01-cumprimentos");
const checks = [];
const add = (name, status, details = "") => checks.push({ name, status, details });

for (const file of ["server.js", "local-responses.js", "local-responses-v2.js"]) {
  const result = spawnSync(process.execPath, ["--check", join(backendDir, file)], { encoding: "utf8" });
  add(`syntax:${file}`, result.status === 0 ? "PASS" : "FAIL", (result.stderr || result.stdout || "").trim().slice(0, 500));
}
try {
  const entries = await readdir(audioDir, { withFileTypes: true });
  const files = entries.filter(entry => entry.isFile() && !/\.(zip|json|md|txt)$/i.test(entry.name));
  let tiny = 0;
  let extensionless = 0;
  for (const file of files) {
    const info = await stat(join(audioDir, file.name));
    if (info.size < 1000) tiny++;
    if (!/\.[a-z0-9]{2,5}$/i.test(file.name)) extensionless++;
  }
  add("audio-directory", files.length > 0 ? "PASS" : "FAIL", `${files.length} arquivos; ${extensionless} sem extensão; ${tiny} abaixo de 1 KB`);
  if (tiny) add("audio-size", "WARN", "Revise os arquivos muito pequenos; eles podem não ser gravações válidas.");
  const catalog = JSON.parse(await readFile(join(rootDir, "media", "audios", "Ayla Logic Enterex - catalog.json"), "utf8"));
  add("audio-catalog", catalog.categories && Object.keys(catalog.categories).length >= 4 ? "PASS" : "FAIL", `${Object.keys(catalog.categories || {}).length} categorias declaradas`);
} catch (error) {
  add("audio-inventory", "FAIL", String(error?.message || error).slice(0, 300));
}
const requiredEnv = ["TELEGRAM_BOT_TOKEN", "TELEGRAM_WEBHOOK_SECRET", "TELEGRAM_WEBHOOK_BASE_URL", "AI_API_KEY"];
const env = Object.fromEntries(requiredEnv.map(key => [key, Boolean(process.env[key]?.trim())]));
add("runtime-env", requiredEnv.every(key => env[key]) ? "PASS" : "WARN", requiredEnv.filter(key => !env[key]).join(", ") ? "Ausentes (normal no CI sem secrets): " + requiredEnv.filter(key => !env[key]).join(", ") : "Todas as variáveis essenciais estão configuradas.");
const report = {
  generatedAt: new Date().toISOString(),
  node: process.version,
  repository: "enterexsystem-blip/-influencer-IA",
  summary: {
    pass: checks.filter(x => x.status === "PASS").length,
    warn: checks.filter(x => x.status === "WARN").length,
    fail: checks.filter(x => x.status === "FAIL").length
  },
  checks
};
console.log(JSON.stringify(report, null, 2));
if (process.env.DIAGNOSTICS_REPORT_PATH) await writeFile(process.env.DIAGNOSTICS_REPORT_PATH, JSON.stringify(report, null, 2) + "\n");
if (report.summary.fail) process.exitCode = 1;
