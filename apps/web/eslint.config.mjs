import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTypescript from 'eslint-config-next/typescript';

// Sin estilos en línea: la política de contenido los bloquea en el HTML del servidor (doc 03).
const sinEstilo = { selector: "JSXAttribute[name.name='style']", message: 'Nada de style="": la CSP lo bloquea. Usa una clase o pon el valor desde una ref.' };
// «Reintentar» tiene que volver a pedir los datos: `retry` lo hace. `reset` solo repinta lo
// que ya falló (regla heredada de lk-crm).
const sinReset = { selector: "Identifier[name='reset']", message: 'En una página de error se reintenta con `retry`, que vuelve a pedir los datos; `reset` solo repinta el error.' };

const sinEstilosEnLinea = {
  files: ['app/**/*.tsx', 'components/**/*.tsx'],
  rules: { 'no-restricted-syntax': ['error', sinEstilo] },
};
// Va después: en las páginas de error valen las dos reglas.
const paginasDeError = {
  files: ['app/**/error.tsx', 'app/global-error.tsx'],
  rules: { 'no-restricted-syntax': ['error', sinEstilo, sinReset] },
};

const config = [
  { ignores: ['.next/**', 'next-env.d.ts', 'test-results/**', 'playwright-report/**', 'public/**'] },
  ...nextVitals,
  ...nextTypescript,
  sinEstilosEnLinea,
  paginasDeError,
];

export default config;
