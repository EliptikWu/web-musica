#!/usr/bin/env node
/**
 * Corre en local lo mismo que correría el CI (doc 09, «Puertas de calidad»).
 * Uso: npm run check                 todo, con extremo a extremo
 *      npm run check -- --sin-e2e    sin Playwright (más rápido)
 * Se detiene en el primer paso que falla y dice cuál fue.
 */
import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const sinE2e = process.argv.includes('--sin-e2e');
const env = { ...process.env, NEXT_PUBLIC_API_MODE: 'simulado' };

const pasos = [
  ['Contrato: generar lo que lee el backend', 'npm', ['run', 'generar', '-w', '@rockola/contrato']],
  ['Contrato: lo generado está al día en git', generadoAlDia],
  ['Contrato: tipos', 'npm', ['run', 'typecheck', '-w', '@rockola/contrato']],
  ['Contrato: pruebas', 'npm', ['run', 'test', '-w', '@rockola/contrato']],
  ['UI: tipos', 'npm', ['run', 'typecheck', '-w', '@rockola/ui']],
  ['Web: estilo (ESLint)', 'npm', ['run', 'lint', '-w', '@rockola/web']],
  ['Web: tipos', 'npm', ['run', 'typecheck', '-w', '@rockola/web']],
  ['Web: pruebas de lógica (Vitest)', 'npm', ['run', 'test', '-w', '@rockola/web']],
  ['Web: compilación', 'npm', ['run', 'build', '-w', '@rockola/web']],
  ...(sinE2e ? [] : [['Web: extremo a extremo + accesibilidad (Playwright)', 'npm', ['run', 'e2e', '-w', '@rockola/web', '--', '--project=e2e']]]),
  ['Dependencias: sin vulnerabilidades altas', 'npm', ['audit', '--omit=dev', '--audit-level=high']],
];

/** Sin cambios ni archivos nuevos sin seguimiento en generado/: lo que lee el backend está en git. */
function generadoAlDia() {
  const r = spawnSync('git', ['status', '--porcelain', '--', 'packages/contrato/generado'], { cwd: raiz, encoding: 'utf8' });
  if (r.status !== 0) return 1;
  // «XY ruta»: Y es el árbol de trabajo. Lo ya preparado (X) cuenta como al día; lo que cambió
  // al generar o no está en git (??) no.
  const sinGuardar = r.stdout.split('\n').filter((l) => l.length > 3 && (l.startsWith('??') || l[1] !== ' '));
  if (sinGuardar.length > 0) {
    process.stdout.write(`Lo generado no está en git:\n${sinGuardar.join('\n')}\n`);
    return 1;
  }
  return 0;
}

const inicio = Date.now();
for (const [nombre, comando, argumentos] of pasos) {
  process.stdout.write(`\n▶ ${nombre}\n`);
  const r =
    typeof comando === 'function'
      ? { status: comando() }
      : spawnSync(comando, argumentos, { cwd: raiz, env, stdio: 'inherit', shell: process.platform === 'win32' });
  if (r.status !== 0) {
    process.stdout.write(`\n✗ Falló: ${nombre}\n`);
    process.exit(r.status ?? 1);
  }
}
process.stdout.write(`\n✓ Todo en verde (${Math.round((Date.now() - inicio) / 1000)} s)${sinE2e ? ', sin extremo a extremo' : ''}\n`);
