import { defineConfig, devices } from '@playwright/test'

// * Se corre con `pnpm test:e2e` (scripts/e2e/e2e.mjs), que levanta el entorno desechable.
export default defineConfig({
  testDir: '.',
  outputDir: '../test-results',
  fullyParallel: false,
  workers: 1,
  // ! Sin reintentos, tampoco en CI: una prueba inestable se corrige, no se tapa.
  retries: 0,
  forbidOnly: Boolean(process.env.CI),
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { outputFolder: '../playwright-report', open: 'never' }]],
  use: {
    baseURL: process.env.E2E_WEB_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], locale: 'es-ES' } }],
})
