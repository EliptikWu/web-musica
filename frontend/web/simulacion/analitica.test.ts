import { ComparacionAnalitica, CuboAnalitica, DiagnosticoAnalitica, ParticipacionAnalitica, Recomendacion, ResumenAnalitica } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { comparar, cubo, cuboVivo, diagnostico, nochesDeEjemplo, pedidasEnEjemplo, participacion, recomendaciones, resumen, seleccionar, UMBRALES_POR_DEFECTO } from './analitica';
import { categoriaDe, corregirCategoria, listarCatalogo } from './categorias';
import { crearEstado } from './datos/semilla';
import { ahoraSim } from './motor';

const estado = crearEstado('momento-b', 'persona', 1_000_000);
const todas = nochesDeEjemplo(estado);
const una = (fecha: string) => seleccionar(todas, { noche: `n-${fecha}` })!;

describe('las noches de ejemplo (RNF-015)', () => {
  it('siguen el horario (jueves a sábado) y el calendario, y terminan el 21 de octubre', () => {
    const dias = new Set(todas.filter((n) => !n.noche.evento).map((n) => n.noche.dia_semana));
    expect([...dias].sort()).toEqual([4, 5, 6]);
    expect(todas.find((n) => n.noche.id === 'n-2026-10-21')?.noche.evento?.tipo_evento).toBe('karaoke');
    expect(todas.find((n) => n.noche.id === 'n-2026-10-18')?.noche.evento?.tipo_evento).toBe('partido');
    expect(todas.at(-1)!.noche.id).toBe('n-2026-10-21');
  });

  it('salen siempre iguales', () => {
    const otra = crearEstado('momento-a', 'persona', 5_000_000);
    expect(JSON.stringify(nochesDeEjemplo(otra).map((n) => n.solicitudes.length))).toBe(JSON.stringify(todas.map((n) => n.solicitudes.length)));
  });

  it('cada pedido suena después de pedirse o tiene su causa; lo que suena no se pisa', () => {
    for (const n of todas) {
      for (const s of n.solicitudes) {
        if (s.estado === 'sono' || s.estado === 'omitida') expect(s.inicio).toBeGreaterThanOrEqual(s.pedida);
        else expect(s.inicio).toBeNull();
      }
      for (let i = 1; i < n.reproducciones.length; i++) expect(n.reproducciones[i]!.inicio).toBeGreaterThan(n.reproducciones[i - 1]!.inicio);
      // Lo que sonó (pedidos y lista de la casa) llena la noche: no hay silencios.
      expect(n.pedidos_seg + n.casa_seg).toBeGreaterThanOrEqual((n.fin - n.inicio) / 1000 - 600);
    }
  });
});

describe('tablero descriptivo (RF-049, RF-050)', () => {
  it('cuadra con los registros de la noche', () => {
    const noche = una('2026-10-17');
    const r = ResumenAnalitica.parse(resumen(todas, noche));
    expect(r.solicitudes).toBe(noche[0]!.solicitudes.length);
    expect(r.votos).toBe(noche[0]!.solicitudes.reduce((s, x) => s + x.votos.length, 0));
    expect(r.no_sonaron).toBe(noche[0]!.solicitudes.filter((s) => s.estado !== 'sono').length);
    expect(r.historial.length).toBe(noche[0]!.reproducciones.length);
    expect(r.espera!.maxima_seg).toBeGreaterThanOrEqual(r.espera!.promedio_seg);
    expect(r.datos).toMatchObject({ origen: 'ejemplo', noches: 1 });
  });

  it('en un rango no trae historial y suma todas las noches', () => {
    const rango = seleccionar(todas, { desde: '2026-10-01', hasta: '2026-10-21' })!;
    const r = resumen(todas, rango);
    expect(r.historial).toEqual([]);
    expect(r.solicitudes).toBe(rango.reduce((s, n) => s + n.solicitudes.length, 0));
  });
});

describe('participación (RF-051)', () => {
  it('géneros con «sin clasificar» al final y franjas desde la tarde', () => {
    const p = ParticipacionAnalitica.parse(participacion(todas, todas));
    expect(p.generos.at(-1)!.genero).toBeNull();
    expect(p.generos[0]!.solicitudes).toBeGreaterThanOrEqual(p.generos[1]!.solicitudes);
    // Desde el partido de la tarde hasta la madrugada, en el orden de la noche.
    const desdeMediodia = p.franjas.map((f) => (f.hora + 12) % 24);
    expect(desdeMediodia).toEqual([...desdeMediodia].sort((a, b) => a - b));
    expect(p.franjas[0]!.hora).toBeLessThan(20);
    expect(p.franjas.at(-1)!.hora).toBe(1);
    expect(p.generos.reduce((s, g) => s + g.solicitudes, 0)).toBe(resumen(todas, todas).solicitudes);
  });

  it('filtra por día de la semana', () => {
    const viernes = seleccionar(todas, { dia_semana: '5' })!;
    expect(viernes.length).toBeGreaterThan(3);
    expect(viernes.every((n) => n.noche.dia_semana === 5)).toBe(true);
  });
});

describe('diagnóstico (RF-052)', () => {
  it('marca la noche floja y la concentración de votos con los umbrales', () => {
    const d = DiagnosticoAnalitica.parse(diagnostico(todas, todas, UMBRALES_POR_DEFECTO));
    const floja = d.noches.find((n) => n.noche_id === 'n-2026-10-01')!;
    expect(floja.telefonos).toBeLessThan(UMBRALES_POR_DEFECTO.participacion_minima);
    expect(d.no_sonaron.every((x) => x.votos >= UMBRALES_POR_DEFECTO.votos_minimos)).toBe(true);
    const barra = diagnostico(todas, una('2026-10-10'), UMBRALES_POR_DEFECTO);
    expect(barra.concentracion.pct_votos_del_10).toBeGreaterThan(UMBRALES_POR_DEFECTO.concentracion_pct);
  });
});

describe('la zona del bar', () => {
  it('las noches y sus horas siguen la zona del establecimiento', () => {
    const lima = crearEstado('momento-b', 'persona', 1_000_000);
    lima.bar.zona_horaria = 'America/Mexico_City';
    const noches = nochesDeEjemplo(lima);
    // 20:00 en Ciudad de México es 02:00 UTC del día siguiente (UTC−6).
    expect(noches[0]!.noche.inicio).toBe('2026-09-11T02:00:00.000Z');
    expect(participacion(noches, noches).franjas.some((f) => f.hora === 20)).toBe(true);
  });

  it('un teléfono vota una vez por solicitud y nunca lo suyo', () => {
    for (const n of todas) for (const s of n.solicitudes) {
      const quienes = s.votos.map((v) => v.dispositivo);
      expect(new Set(quienes).size).toBe(quienes.length);
      expect(quienes).not.toContain(s.dispositivo);
    }
  });
});

describe('comparar (RF-053)', () => {
  it('agrupa por día y por tipo de evento, en promedio por noche', () => {
    const porDia = ComparacionAnalitica.parse(comparar(todas, todas, 'dia'));
    expect(porDia.grupos.map((g) => g.etiqueta)).toEqual(['Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']);
    const porEvento = comparar(todas, todas, 'evento');
    expect(porEvento.grupos.map((g) => g.etiqueta)).toEqual(['Noche normal', 'Karaoke', 'Partido']);
    const sabados = todas.filter((n) => n.noche.dia_semana === 6);
    const promedio = sabados.reduce((s, n) => s + n.solicitudes.length, 0) / sabados.length;
    expect(porDia.grupos.find((g) => g.clave === '6')!.solicitudes).toBeCloseTo(promedio, 1);
  });
});

describe('recomendaciones (RF-054)', () => {
  it('cada una dice en qué se basa y a dónde ir; ninguna toca el estado', () => {
    const antes = JSON.stringify(estado);
    const rs = recomendaciones(estado, todas, ahoraSim(estado, 1_000_000)).map((r) => Recomendacion.parse(r));
    expect(JSON.stringify(estado)).toBe(antes);
    expect(rs.map((r) => r.tipo)).toEqual(expect.arrayContaining(['agregar_a_lista', 'revisar_bloqueo']));
    for (const r of rs) expect(r.base.length).toBeGreaterThan(20);
  });
});

describe('RF-067: el cubo en vivo de la simulación', () => {
  const ahora = ahoraSim(estado, 1_000_000);
  const genero = (video: string) => categoriaDe(estado, video).categoria ?? 'Sin clasificar';
  const total = (c: CuboAnalitica) => c.celdas.find((x) => x.genero === '*' && x.franja === '*' && x.tercero === '*')!;

  it('suma la noche abierta a las de ejemplo y lo dice', () => {
    const vivo = cuboVivo(estado, ahora, 'dia', genero);
    const historico = cubo(todas, 'dia');
    const deHoy = estado.solicitudes.length;
    expect(deHoy).toBeGreaterThan(0);
    expect(total(vivo).solicitudes).toBe(total(historico).solicitudes + deHoy);
    expect(vivo.datos).toMatchObject({ en_vivo: true, fuente: 'operativa', origen: 'ejemplo', sesiones_abiertas: 1 });
    expect(CuboAnalitica.safeParse(vivo).success).toBe(true);
  });

  it('sin tope de géneros ni «Otros», y los días en orden de la semana', () => {
    const vivo = cuboVivo(estado, ahora, 'dia', genero);
    expect(vivo.miembros.genero).not.toContain('Otros');
    expect(vivo.miembros.genero.length).toBeGreaterThan(4);
    const semana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
    expect(vivo.miembros.tercero).toEqual(semana.filter((d) => vivo.miembros.tercero.includes(d)));
  });

  it('un género corregido a mano cuenta de inmediato', () => {
    const copia = structuredClone(estado);
    const video = copia.solicitudes[0]!.video_id;
    const r = corregirCategoria(copia, pedidasEnEjemplo(copia), ahora, { id: 'u-admin', nombre: 'Admin' }, video, 'rock');
    expect(r).toMatchObject({ ok: true, cuerpo: { categoria: 'Rock', origen_categoria: 'MANUAL' } });
    expect(copia.acciones.at(-1)?.tipo).toBe('editar_categoria');
    const vivo = cuboVivo(copia, ahora, 'dia', (v) => categoriaDe(copia, v).categoria ?? 'Sin clasificar');
    expect(vivo.miembros.genero).toContain('Rock');
  });

  it('el catálogo rechaza un género que no existe y una canción ajena al bar', () => {
    const quien = { id: 'u-admin', nombre: 'Admin' };
    expect(corregirCategoria(estado, new Map(), ahora, quien, estado.solicitudes[0]!.video_id, 'Polka')).toMatchObject({ ok: false, codigo: 'datos_invalidos' });
    expect(corregirCategoria(estado, new Map(), ahora, quien, 'sim-no-existe', 'Salsa')).toMatchObject({ ok: false, codigo: 'no_encontrado' });
  });

  it('el catálogo pone primero lo que no tiene género y filtra', () => {
    const todasLasDelBar = listarCatalogo(estado, pedidasEnEjemplo(estado), { soloSinCategoria: false, q: '', limite: 100 });
    const primeraConGenero = todasLasDelBar.findIndex((c) => c.categoria !== null);
    expect(todasLasDelBar.slice(primeraConGenero).every((c) => c.categoria !== null)).toBe(true);
    const sin = listarCatalogo(estado, pedidasEnEjemplo(estado), { soloSinCategoria: true, q: '', limite: 100 });
    expect(sin.length).toBeGreaterThan(0);
    expect(sin.every((c) => c.categoria === null && c.origen_categoria === null)).toBe(true);
  });
});
