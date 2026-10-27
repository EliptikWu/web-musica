'use client';

import type { Rol, UsuarioPanel } from '@rockola/contrato';
import { Campo, Dialogo, GrupoTeclas, Selector, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { nuevaClave } from '@/lib/api/hooks-cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { conPunto, fechaYHora } from '@/lib/formato';
import { ROL } from '@/lib/textos/panel';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

const ROLES = [
  { valor: 'operador', texto: 'Operador' },
  { valor: 'administrador', texto: 'Administrador' },
] as const;

type Edicion = { tipo: 'crear' } | { tipo: 'editar'; usuario: UsuarioPanel } | null;

/**
 * Personal del bar (RF-023 a RF-026, PB-23, PB-24): crear con clave temporal (se muestra una
 * vez y obliga a cambiarla), editar, restablecer la clave y desactivar. Nunca se ven claves;
 * no se puede dejar el bar sin Administrador ni desactivarse uno mismo.
 */
export function Usuarios() {
  const yo = useYoDelPanel();
  const [q, setQ] = useState('');
  const [rol, setRol] = useState<'' | Rol>('');
  const [estado, setEstado] = useState<'' | 'activo' | 'inactivo'>('');
  const usuarios = useQuery({
    queryKey: [...claves.panel.usuarios, { q, rol, estado }],
    queryFn: ({ signal }) => pedir('usuarios', { consulta: { q: q || undefined, rol: rol || undefined, estado: estado || undefined }, signal }),
  });
  const invalidar = [claves.panel.usuarios];
  const crear = useAccionGestion('crearUsuario', invalidar);
  // Editarse a uno mismo cambia el nombre de la cabecera o el rol: también `yo`.
  const editar = useAccionGestion('editarUsuario', [...invalidar, claves.panel.yo]);
  const restablecer = useAccionGestion('claveTemporal', invalidar);
  const desactivar = useAccionGestion('desactivarUsuario', invalidar);
  const reactivar = useAccionGestion('reactivarUsuario', invalidar);
  const [edicion, setEdicion] = useState<Edicion>(null);
  const [aRestablecer, setARestablecer] = useState<UsuarioPanel | null>(null);
  const [clave, setClave] = useState<{ nombre: string; clave: string } | null>(null);
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);
  const errorPor = { conflicto: 'No se puede: ese correo ya está en uso, es el último Administrador o eres tú.' };

  return (
    <PaginaGestion
      titulo="Personal"
      nota="Quién entra al panel y con qué rol. El Operador controla la música y la cola; el Administrador, todo."
      accion={
        <Tecla variante="cereza" tamano="ancha" onClick={() => setEdicion({ tipo: 'crear' })}>
          Agregar a alguien
        </Tecla>
      }
    >
      <form className="caja formulario-fila mt-4" role="search" onSubmit={(e) => e.preventDefault()} aria-label="Buscar en el personal">
        <Campo etiqueta="Buscar" type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Nombre o correo" />
        <Selector etiqueta="Rol" value={rol} onChange={(e) => setRol(e.target.value as '' | Rol)}>
          <option value="">Todos</option>
          <option value="operador">Operador</option>
          <option value="administrador">Administrador</option>
        </Selector>
        <Selector etiqueta="Estado" value={estado} onChange={(e) => setEstado(e.target.value as '' | 'activo' | 'inactivo')}>
          <option value="">Todos</option>
          <option value="activo">Activos</option>
          <option value="inactivo">Inactivos</option>
        </Selector>
      </form>
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.texto} /> : null}
      <section className="caja mt-4" aria-label="Personal">
        <Cargando consulta={usuarios}>
          {() =>
            usuarios.data!.usuarios.length === 0 ? (
              <p className="secundario">Nadie coincide con la búsqueda.</p>
            ) : (
              <ul className="lista-gestion">
                {usuarios.data!.usuarios.map((u) => (
                  <li key={u.id}>
                    <div className="lista-gestion__info">
                      <p className="lista-gestion__titulo">
                        {u.nombre}
                        <span className="etiqueta">{ROL[u.rol]}</span>
                        {u.estado === 'inactivo' ? <span className="etiqueta etiqueta--cereza">Inactivo</span> : null}
                        {u.debe_cambiar_clave ? <span className="etiqueta">Clave temporal</span> : null}
                      </p>
                      <p className="lista-gestion__meta">
                        {u.correo}. {u.ultimo_acceso ? conPunto(`Último ingreso: ${fechaYHora(u.ultimo_acceso, yo.establecimiento.zona_horaria)}`) : 'Todavía no ha entrado.'}
                      </p>
                    </div>
                    <div className="lista-gestion__acciones">
                      <Tecla tamano="chica" aria-label={`Editar a ${u.nombre}`} onClick={() => setEdicion({ tipo: 'editar', usuario: u })}>
                        Editar
                      </Tecla>
                      <Tecla tamano="chica" aria-label={`Restablecer la clave de ${u.nombre}`} onClick={() => setARestablecer(u)}>
                        Clave
                      </Tecla>
                      {u.id !== yo.usuario.id ? (
                        u.estado === 'activo' ? (
                          <Tecla
                            tamano="chica"
                            aria-label={`Desactivar a ${u.nombre}`}
                            onClick={() =>
                              desactivar.mutate(
                                { parametros: { id: u.id } },
                                { onSuccess: () => setAviso({ tipo: 'luz', texto: `${u.nombre} ya no puede entrar al panel.` }), onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, errorPor) }) },
                              )
                            }
                          >
                            Desactivar
                          </Tecla>
                        ) : (
                          <Tecla
                            tamano="chica"
                            aria-label={`Reactivar a ${u.nombre}`}
                            onClick={() =>
                              reactivar.mutate(
                                { parametros: { id: u.id } },
                                { onSuccess: () => setAviso({ tipo: 'luz', texto: `${u.nombre} puede volver a entrar.` }), onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }) },
                              )
                            }
                          >
                            Reactivar
                          </Tecla>
                        )
                      ) : null}
                    </div>
                  </li>
                ))}
              </ul>
            )
          }
        </Cargando>
      </section>

      <DialogoUsuario
        key={edicion?.tipo === 'editar' ? edicion.usuario.id : edicion?.tipo ?? 'cerrado'}
        edicion={edicion}
        ocupado={crear.isPending || editar.isPending}
        onCerrar={() => setEdicion(null)}
        onGuardar={(datos) => {
          if (edicion?.tipo === 'crear') {
            crear.mutate(
              { cuerpo: datos, idempotencia: nuevaClave() },
              {
                onSuccess: (r) => {
                  setEdicion(null);
                  setClave({ nombre: r.usuario.nombre, clave: r.clave_temporal });
                },
                onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, errorPor) }),
              },
            );
          } else if (edicion?.tipo === 'editar') {
            editar.mutate(
              { parametros: { id: edicion.usuario.id }, cuerpo: datos },
              { onSuccess: () => setAviso({ tipo: 'luz', texto: 'Cambios guardados.' }), onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, errorPor) }), onSettled: () => setEdicion(null) },
            );
          }
        }}
      />
      <DialogoConfirmar
        abierto={aRestablecer !== null}
        titulo={`¿Restablecer la clave de ${aRestablecer?.nombre ?? ''}?`}
        texto="Se le cierra la sesión y tendrá que entrar con una clave temporal y cambiarla."
        confirmar="Restablecer"
        ocupado={restablecer.isPending}
        onCerrar={() => setARestablecer(null)}
        onConfirmar={() =>
          aRestablecer &&
          restablecer.mutate(
            { parametros: { id: aRestablecer.id } },
            { onSuccess: (r) => setClave({ nombre: aRestablecer.nombre, clave: r.clave_temporal }), onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }), onSettled: () => setARestablecer(null) },
          )
        }
      />
      <Dialogo abierto={clave !== null} titulo={`Clave temporal de ${clave?.nombre ?? ''}`} onCerrar={() => setClave(null)}>
        <p className="dialogo__texto">Dásela en persona. Solo se muestra ahora: al entrar tendrá que cambiarla.</p>
        <p className="ventana px-4 py-3 text-[22px] font-bold tracking-wide select-all">{clave?.clave}</p>
        <div className="dialogo__acciones">
          <Tecla variante="cereza" onClick={() => setClave(null)}>
            Listo
          </Tecla>
        </div>
      </Dialogo>
    </PaginaGestion>
  );
}

function DialogoUsuario({
  edicion,
  ocupado,
  onCerrar,
  onGuardar,
}: {
  edicion: Edicion;
  ocupado: boolean;
  onCerrar: () => void;
  onGuardar: (datos: { nombre: string; correo: string; rol: Rol }) => void;
}) {
  const inicial = edicion?.tipo === 'editar' ? edicion.usuario : null;
  const [nombre, setNombre] = useState(inicial?.nombre ?? '');
  const [correo, setCorreo] = useState(inicial?.correo ?? '');
  const [rol, setRol] = useState<Rol>(inicial?.rol ?? 'operador');
  const [intento, setIntento] = useState(false);
  const correoValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim());
  return (
    <Dialogo abierto={edicion !== null} titulo={edicion?.tipo === 'crear' ? 'Agregar a alguien del personal' : `Editar a ${inicial?.nombre ?? ''}`} onCerrar={onCerrar}>
      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          setIntento(true);
          if (!nombre.trim() || !correoValido) return;
          onGuardar({ nombre: nombre.trim(), correo: correo.trim(), rol });
        }}
      >
        <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} error={intento && !nombre.trim() ? 'Escribe el nombre.' : null} autoFocus />
        <Campo etiqueta="Correo" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} error={intento && !correoValido ? 'Escribe un correo válido.' : null} />
        <GrupoTeclas leyenda="Rol" nombre="rol" opciones={[...ROLES]} valor={rol} onCambio={setRol} />
        <div className="dialogo__acciones">
          <Tecla type="button" onClick={onCerrar}>
            Cancelar
          </Tecla>
          <Tecla type="submit" variante="cereza" ocupada={ocupado}>
            {edicion?.tipo === 'crear' ? 'Agregar' : 'Guardar'}
          </Tecla>
        </div>
      </form>
    </Dialogo>
  );
}
