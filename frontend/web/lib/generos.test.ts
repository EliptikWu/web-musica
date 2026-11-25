import { describe, expect, it } from 'vitest';
import { ordenarGeneros, resumenReclasificacion, textoOrigen } from './generos';

describe('géneros en «Canciones» (RF-068)', () => {
  it('van en orden alfabético en español, sin tocar la lista original', () => {
    const del_servidor = ['Salsa', 'Urbano', 'Música Llanera', 'Bachata', 'Ñapa', 'Ópera'];
    expect(ordenarGeneros(del_servidor)).toEqual(['Bachata', 'Música Llanera', 'Ñapa', 'Ópera', 'Salsa', 'Urbano']);
    expect(del_servidor[0]).toBe('Salsa');
  });

  it('una canción vieja con género pero sin origen no dice «Nadie le ha puesto género»', () => {
    expect(textoOrigen('Salsa', null)).toBe('Automático, de antes.');
    expect(textoOrigen(null, null)).toBe('Nadie le ha puesto género.');
    expect(textoOrigen('Salsa', 'MANUAL')).toBe('Puesto a mano.');
  });
});

describe('«Clasificar las que faltan» (RF-068, punto 18)', () => {
  it('dice cuántas encontró y qué hacer con las demás', () => {
    expect(resumenReclasificacion({ clasificadas: 3, sinPistas: 1, noDisponibles: 0, interrumpida: null })).toEqual({
      tipo: 'luz',
      titulo: 'Se encontró el género de 3 canciones.',
      meta: '1 no tiene pistas en YouTube: ponles el género a mano.',
    });
  });
  it('sin cuota de YouTube y sin ninguna encontrada es un error que se puede reintentar', () => {
    const r = resumenReclasificacion({ clasificadas: 0, sinPistas: 0, noDisponibles: 0, interrumpida: 'cuota_agotada' });
    expect(r.tipo).toBe('error');
    expect(r.meta).toContain('mañana');
  });
});
