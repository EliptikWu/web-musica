import { z } from 'zod';
import { Franja } from './bar.ts';
import { Id, Slug } from './comunes.ts';
import { Rol } from './panel.ts';

/**
 * La plataforma (RF-064, ADR-29): quien administra Rockola crea bares y sus cuentas. Es una
 * superficie aparte del panel: otra tabla de cuentas y otra cookie, sin acceso a la operación
 * de ningún bar.
 */
export const YoPlataforma = z
  .object({ id: Id, nombre: z.string().min(1), correo: z.string().min(1), debe_cambiar_clave: z.boolean() })
  .meta({ id: 'YoPlataforma' });
export type YoPlataforma = z.infer<typeof YoPlataforma>;

export const CuentaBar = z
  .object({
    id: Id,
    nombre: z.string().min(1),
    // Al leer no se exige formato: una cuenta antigua no debe tumbar la lista de todos los bares.
    correo: z.string().min(1),
    rol: Rol,
    estado: z.enum(['activo', 'inactivo']),
    debe_cambiar_clave: z.boolean(),
  })
  .meta({ id: 'CuentaBar' });
export type CuentaBar = z.infer<typeof CuentaBar>;

export const BarPlataforma = z
  .object({
    id: Id,
    slug: Slug,
    nombre: z.string().min(1),
    zona_horaria: z.string().describe('IANA, p. ej. America/Bogota'),
    // Con valor por defecto: la web y el backend no se despliegan a la vez, y un backend anterior
    // sin estos dos campos no debe tumbar la lista de bares.
    direccion: z.string().default('').describe('Vacía si el bar no la ha escrito'),
    horario: z.array(Franja).default([]).describe('Vacío si el bar no lo ha definido'),
    activo: z.boolean(),
    cuentas: z.array(CuentaBar),
  })
  .meta({ id: 'BarPlataforma' });
export type BarPlataforma = z.infer<typeof BarPlataforma>;

export const ListaBaresPlataforma = z.object({ bares: z.array(BarPlataforma) }).meta({ id: 'ListaBaresPlataforma' });

export const CuerpoCuentaBar = z
  .object({ nombre: z.string().trim().min(1).max(120), correo: z.email().max(254), rol: Rol })
  .meta({ id: 'CuerpoCuentaBar' });

export const CuerpoBarNuevo = z
  .object({
    nombre: z.string().trim().min(1).max(150),
    slug: Slug.max(100),
    zona_horaria: z.string().min(1).max(60),
    administrador: z.object({ nombre: z.string().trim().min(1).max(120), correo: z.email().max(254) }),
  })
  .meta({ id: 'CuerpoBarNuevo', description: 'El bar nace con su primer Administrador' });

export const CuerpoClavePlataforma = z
  .object({ actual: z.string().min(1), nueva: z.string().min(12, 'Al menos 12 caracteres') })
  .meta({ id: 'CuerpoClavePlataforma', description: 'Mínimo de 12 caracteres (propuesto)' });

export const BarCreado = z
  .object({ bar: BarPlataforma, clave_temporal: z.string().describe('Del primer Administrador. Se muestra una sola vez') })
  .meta({ id: 'BarCreado' });

export const ClaveCuentaBar = z
  .object({ clave_temporal: z.string().describe('Se muestra una sola vez') })
  .meta({ id: 'ClaveCuentaBar' });

export const CuentaCreada = z
  .object({ cuenta: CuentaBar, clave_temporal: z.string().describe('Se muestra una sola vez') })
  .meta({ id: 'CuentaCreada' });
