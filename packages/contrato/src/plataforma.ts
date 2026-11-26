import * as z from 'zod/mini';
import { Franja } from './bar.ts';
import { Id, Slug } from './comunes.ts';
import { Rol } from './panel.ts';

/**
 * La plataforma (RF-064, ADR-29): quien administra Rockola crea bares y sus cuentas. Es una
 * superficie aparte del panel: otra tabla de cuentas y otra cookie, sin acceso a la operación
 * de ningún bar.
 */
export const YoPlataforma = z
  .object({ id: Id, nombre: z.string().check(z.minLength(1)), correo: z.string().check(z.minLength(1)), debe_cambiar_clave: z.boolean() }).register(z.globalRegistry, { id: 'YoPlataforma' });
export type YoPlataforma = z.infer<typeof YoPlataforma>;

export const CuentaBar = z
  .object({
    id: Id,
    nombre: z.string().check(z.minLength(1)),
    // Al leer no se exige formato: una cuenta antigua no debe tumbar la lista de todos los bares.
    correo: z.string().check(z.minLength(1)),
    rol: Rol,
    estado: z.enum(['activo', 'inactivo']),
    debe_cambiar_clave: z.boolean(),
  }).register(z.globalRegistry, { id: 'CuentaBar' });
export type CuentaBar = z.infer<typeof CuentaBar>;

export const BarPlataforma = z
  .object({
    id: Id,
    slug: Slug,
    nombre: z.string().check(z.minLength(1)),
    zona_horaria: z.string().check(z.describe('IANA, p. ej. America/Bogota')),
    // Con valor por defecto: la web y el backend no se despliegan a la vez, y un backend anterior
    // sin estos dos campos no debe tumbar la lista de bares.
    direccion: z._default(z.string(), '').check(z.describe('Vacía si el bar no la ha escrito')),
    horario: z._default(z.array(Franja), []).check(z.describe('Vacío si el bar no lo ha definido')),
    activo: z.boolean(),
    cuentas: z.array(CuentaBar),
  }).register(z.globalRegistry, { id: 'BarPlataforma' });
export type BarPlataforma = z.infer<typeof BarPlataforma>;

export const ListaBaresPlataforma = z.object({ bares: z.array(BarPlataforma) }).register(z.globalRegistry, { id: 'ListaBaresPlataforma' });

export const CuerpoCuentaBar = z
  .object({ nombre: z.string().check(z.trim()).check(z.minLength(1)).check(z.maxLength(120)), correo: z.email().check(z.maxLength(254)), rol: Rol }).register(z.globalRegistry, { id: 'CuerpoCuentaBar' });

export const CuerpoBarNuevo = z
  .object({
    nombre: z.string().check(z.trim()).check(z.minLength(1)).check(z.maxLength(150)),
    slug: Slug.check(z.maxLength(100)),
    zona_horaria: z.string().check(z.minLength(1)).check(z.maxLength(60)),
    administrador: z.object({ nombre: z.string().check(z.trim()).check(z.minLength(1)).check(z.maxLength(120)), correo: z.email().check(z.maxLength(254)) }),
  }).register(z.globalRegistry, { id: 'CuerpoBarNuevo', description: 'El bar nace con su primer Administrador' });

export const CuerpoClavePlataforma = z
  .object({ actual: z.string().check(z.minLength(1)), nueva: z.string().check(z.minLength(12, 'Al menos 12 caracteres')) }).register(z.globalRegistry, { id: 'CuerpoClavePlataforma', description: 'Mínimo de 12 caracteres (propuesto)' });

export const BarCreado = z
  .object({ bar: BarPlataforma, clave_temporal: z.string().check(z.describe('Del primer Administrador. Se muestra una sola vez')) }).register(z.globalRegistry, { id: 'BarCreado' });

export const ClaveCuentaBar = z
  .object({ clave_temporal: z.string().check(z.describe('Se muestra una sola vez')) }).register(z.globalRegistry, { id: 'ClaveCuentaBar' });

export const CuentaCreada = z
  .object({ cuenta: CuentaBar, clave_temporal: z.string().check(z.describe('Se muestra una sola vez')) }).register(z.globalRegistry, { id: 'CuentaCreada' });
