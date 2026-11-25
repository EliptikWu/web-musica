/** Textos de la plataforma (RF-064). Español de Colombia, en la voz del panel. */
export const plataforma = {
  marca: 'Administración de bares',
  elegirBar: 'Ir a un bar',
  todosLosBares: (n: number) => `Todos los bares (${n})`,
  cargando: 'Cargando la plataforma…',
  noCargo: 'La plataforma no cargó',
  errorGenerico: 'Revisa la conexión e inténtalo de nuevo.',
  noSalio: 'No se pudo cerrar la sesión. Inténtalo de nuevo.',
  entrar: 'Entrar a la plataforma',
  credenciales: 'Correo o clave incorrectos. Tras 5 intentos fallidos, la cuenta espera 15 minutos.',
  simulacion: 'Simulación: superadmin@rockola.demo, clave rockola-demo.',
  cambiarClave: 'Cambia tu clave',
  cambiarClaveNota: 'La clave inicial solo sirve para entrar la primera vez. Elige una de al menos 12 caracteres.',
  claveCorta: 'La clave nueva debe tener al menos 12 caracteres y ser distinta de la actual.',
  noCoinciden: 'Las dos claves nuevas no coinciden.',
  actualMal: 'La clave actual no coincide.',
  titulo: 'Bares',
  nota: 'Crea bares y administra las cuentas de cada uno.',
  crearBar: 'Crear bar',
  agregarCuenta: 'Agregar cuenta',
  direccion: 'Dirección',
  horario: 'Horario',
  sinDireccion: 'Sin dirección',
  sinHorario: 'Sin horario',
  cuentas: 'Cuentas',
  verCuentas: (n: number) => (n === 1 ? 'Ver 1 cuenta' : `Ver ${n} cuentas`),
  ocultarCuentas: 'Ocultar cuentas',
  vacio: 'Todavía no hay bares. Crea el primero para entregarle su acceso.',
  sinCuentas: 'Este bar no tiene cuentas. Agrega su Administrador para que pueda entrar.',
  nuevaClave: 'Clave nueva',
  nuevaClaveNota: 'Se cierra su sesión y la clave anterior deja de servir. Tendrá que cambiar la nueva al entrar.',
  cuentaNoExiste: 'Esa cuenta ya no existe.',
  primerAdmin: 'Primer Administrador',
  slugAyuda: (slug: string) => `Los clientes entrarán por /b/${slug || 'nombre-del-bar'}. Después no se cambia desde aquí.`,
  slugMal: 'Usa minúsculas, números y guiones, sin espacios.',
  slugOcupado: 'Ya hay un bar con esa dirección web. Elige otra.',
  correoOcupado: 'Ya hay una cuenta con ese correo en este bar.',
  datosMal: 'Revisa los datos: la zona horaria debe ser como America/Bogota.',
  datosCuenta: 'Revisa el nombre y el correo.',
  claveNota: (bar: string) => `Entrégala en persona. Con ella se entra a ${bar} por /panel y hay que cambiarla la primera vez. Solo se muestra ahora.`,
};

/** «Café La Nota» → `cafe-la-nota`. */
export function aSlug(nombre: string): string {
  return nombre
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 100)
    .replace(/-+$/g, '');
}
