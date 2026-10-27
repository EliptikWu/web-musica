import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // Solo la lógica sin interfaz; lo que se ve lo cubren las pruebas de extremo a extremo (doc 09).
    include: ['lib/**/*.test.ts', 'simulacion/**/*.test.ts'],
    environment: 'node',
  },
});
