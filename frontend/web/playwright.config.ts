import { defineConfig, devices } from '@playwright/test';

const CI = Boolean(process.env.CI);
const canal = CI ? {} : { channel: 'msedge' };

/**
 * Pruebas de extremo a extremo (doc 09) contra la app compilada en modo simulado.
 * Antes: `npm run build -w @rockola/web`. En local usan el Edge instalado; en CI, Chromium.
 * Rockola usa el puerto 3200 para no chocar con otros proyectos en el 3000.
 */
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  fullyParallel: false,
  forbidOnly: CI,
  retries: CI ? 1 : 0,
  reporter: CI ? 'line' : 'list',
  use: {
    baseURL: 'http://localhost:3200',
    trace: 'retain-on-failure',
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
  },
  projects: [
    { name: 'e2e', testIgnore: /capturas\.spec\.ts/, use: { ...devices['Desktop Chrome'], ...canal } },
    { name: 'capturas', testMatch: /capturas\.spec\.ts/, use: { ...canal } },
  ],
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:3200',
    // Nunca reutiliza un servidor en marcha: podría servir una compilación vieja y las pruebas
    // mirarían otra cosa. Si el 3200 está ocupado, falla y lo dice.
    reuseExistingServer: false,
    timeout: 60_000,
    env: { NEXT_PUBLIC_API_MODE: 'simulado' },
  },
});
