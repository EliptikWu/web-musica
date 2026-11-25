import { expect, test } from '@playwright/test';
import { abrir, sinViolaciones, vigilarConsola } from './support';

/** RF-064: la plataforma crea bares con su Administrador y cuentas; una sesión de bar no entra. */
test('crear un bar y una cuenta, con la clave temporal una sola vez', async ({ page }) => {
  const errores = vigilarConsola(page, { permitir: [401, 409] });
  await abrir(page, '/plataforma');
  await expect(page.getByRole('heading', { name: 'Entrar a la plataforma' })).toBeVisible();
  await sinViolaciones(page, 'entrar a la plataforma');
  await page.getByLabel('Correo').fill('superadmin@rockola.demo');
  await page.getByLabel('Clave').fill('equivocada');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'Correo o clave incorrectos' })).toBeVisible();
  await page.getByLabel('Clave').fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page.getByRole('heading', { name: 'Bares', exact: true })).toBeVisible();
  await sinViolaciones(page, 'lista de bares');
  // Los bares llegan plegados, con sus datos a la vista; las cuentas se despliegan.
  const demo = page.getByRole('region').filter({ hasText: '/b/' }).first();
  await expect(demo.getByText('Dirección', { exact: true })).toBeVisible();
  await expect(demo.getByText('Horario', { exact: true })).toBeVisible();
  await expect(demo.getByRole('button', { name: /^Dar una clave nueva/ })).toHaveCount(0);
  const desplegar = demo.getByRole('button', { name: /^Ver \d+ cuentas? de/ });
  await expect(desplegar).toHaveAttribute('aria-expanded', 'false');
  await desplegar.click();
  await expect(demo.getByRole('button', { name: /^Ocultar cuentas de/ })).toHaveAttribute('aria-expanded', 'true');
  await expect(demo.getByRole('button', { name: /^Dar una clave nueva/ }).first()).toBeVisible();
  await sinViolaciones(page, 'bar desplegado');
  await demo.getByRole('button', { name: /^Ocultar cuentas de/ }).click();
  await expect(demo.getByRole('button', { name: /^Dar una clave nueva/ })).toHaveCount(0);
  await page.getByRole('button', { name: 'Crear bar' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Crear bar' });
  await dialogo.getByLabel('Nombre del bar').fill('Café La Nota');
  await expect(dialogo.getByLabel('Dirección web del bar')).toHaveValue('cafe-la-nota');
  await dialogo.getByLabel('Nombre', { exact: true }).fill('Ana Ríos');
  await dialogo.getByLabel('Correo').fill('ana@lanota.test');
  await sinViolaciones(page, 'crear bar');
  await dialogo.getByRole('button', { name: 'Crear bar' }).click();

  const clave = page.getByRole('dialog', { name: 'Clave temporal de Ana Ríos' });
  await expect(clave).toContainText('Solo se muestra ahora');
  await clave.getByRole('button', { name: 'Listo' }).click();
  const bar = page.getByRole('region', { name: 'Café La Nota' });
  await expect(bar).toContainText('ana@lanota.test');

  await bar.getByRole('button', { name: 'Agregar una cuenta a Café La Nota' }).click();
  const cuenta = page.getByRole('dialog', { name: 'Agregar una cuenta a Café La Nota' });
  await cuenta.getByLabel('Nombre').fill('Otra');
  await cuenta.getByLabel('Correo').fill('ana@lanota.test');
  await cuenta.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(cuenta.getByRole('alert')).toContainText('Ya hay una cuenta con ese correo');
  await cuenta.getByLabel('Nombre').fill('Luis');
  await cuenta.getByLabel('Correo').fill('luis@lanota.test');
  await cuenta.getByRole('button', { name: 'Crear cuenta' }).click();
  await page.getByRole('dialog', { name: 'Clave temporal de Luis' }).getByRole('button', { name: 'Listo' }).click();
  await expect(bar).toContainText('luis@lanota.test');
  await page.getByRole('button', { name: 'Dar una clave nueva a Luis, de Café La Nota' }).click();
  await page.getByRole('dialog', { name: '¿Dar una clave nueva a Luis?' }).getByRole('button', { name: 'Clave nueva' }).click();
  await page.getByRole('dialog', { name: 'Clave temporal de Luis' }).getByRole('button', { name: 'Listo' }).click();

  // Con varios bares, se elige uno de la lista y queda solo ese, con sus cuentas (punto 15).
  await page.getByLabel('Ir a un bar').selectOption({ label: 'Café La Nota' });
  await expect(page.getByRole('region', { name: 'Café La Nota' })).toContainText('luis@lanota.test');
  await expect(page.getByRole('region').filter({ hasText: '/b/' })).toHaveCount(1);
  await page.getByLabel('Ir a un bar').selectOption({ index: 0 });
  await expect(page.getByRole('region').filter({ hasText: '/b/' })).not.toHaveCount(1);
  await sinViolaciones(page, 'elegir un bar');
  expect(errores).toEqual([]);
});

test('una sesión del panel de un bar no entra a la plataforma', async ({ page }) => {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill('admin@rockola.demo');
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
  await page.goto('/plataforma');
  await expect(page.getByRole('heading', { name: 'Entrar a la plataforma' })).toBeVisible();
  const estado = await page.evaluate(async () => (await fetch('/api/plataforma/bares')).status);
  expect(estado).toBe(401);
});
