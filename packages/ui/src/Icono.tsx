import type { SVGProps } from 'react';

/**
 * Iconos dibujados a mano para Rockola (doc 05): rejilla de 24, trazo 1,9, puntas
 * redondas. Los ocho primeros vienen de los prototipos; el resto se dibujaron con la
 * misma mano para el panel. Solo los que tienen función: nada de adorno.
 */
const TRAZOS = {
  inicio: <path d="M4.5 10.5 12 4.5l7.5 6V19a1 1 0 0 1-1 1H15v-5.5H9V20H5.5a1 1 0 0 1-1-1z" />,
  buscar: (<><circle cx="10.5" cy="10.5" r="6.5" /><path d="m15.5 15.5 4.5 4.5" /></>),
  cola: <path d="M4.5 7h15M4.5 12h15M4.5 17h9" />,
  borrar: <path d="M7 7l10 10M17 7 7 17" />,
  subir: <path d="M12 19V6M6.5 11.5 12 6l5.5 5.5" />,
  grafica: <path d="M5 19V11M12 19V5M19 19v-5" />,
  libro: <path d="M5 5.5A1.5 1.5 0 0 1 6.5 4H19v14H6.5A1.5 1.5 0 0 0 5 19.5zM5 19.5A1.5 1.5 0 0 0 6.5 21H19" />,
  youtube: (<><rect x="3" y="6" width="18" height="12" rx="3.5" /><path d="m10.5 9.5 4 2.5-4 2.5z" /></>),
  // Dibujados para el panel y los estados nuevos.
  pausa: <path d="M9 6v12M15 6v12" />,
  reproducir: <path d="M8 5.5v13l10-6.5z" />,
  saltar: <path d="M6 6l8 6-8 6zM18 6v12" />,
  bloquear: (<><circle cx="12" cy="12" r="7.5" /><path d="m6.8 6.8 10.4 10.4" /></>),
  retirar: <path d="M5 7h14M9.5 7V5h5v2M7 7l1 12h8l1-12" />,
  pedidos: (<><path d="M4.5 7h9M4.5 12h9M4.5 17h6" /><circle cx="17.5" cy="15.5" r="2.5" /><path d="M20 15.5V6l-2.5 1" /></>),
  usuario: (<><circle cx="12" cy="8.5" r="3.5" /><path d="M5 19.5c1.2-3.2 3.8-5 7-5s5.8 1.8 7 5" /></>),
  salir: <path d="M14 4.5H6.5v15H14M10 12h10M16.5 8.5 20 12l-3.5 3.5" />,
  calendario: (<><rect x="4" y="5.5" width="16" height="14" rx="1.5" /><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4" /></>),
  lista: <path d="M9 7h10.5M9 12h10.5M9 17h10.5M4.5 7h.5M4.5 12h.5M4.5 17h.5" />,
  qr: <path d="M4.5 4.5h5v5h-5zM14.5 4.5h5v5h-5zM4.5 14.5h5v5h-5zM14.5 14.5h2M19.5 14.5v2M14.5 19.5h5M14.5 17h.5" />,
  descargar: <path d="M12 4.5v11M7 11l5 5 5-5M5 19.5h14" />,
  imprimir: <path d="M7 9V4.5h10V9M7 16.5H5a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v5.5a1 1 0 0 1-1 1h-2M7 14h10v6H7z" />,
  alerta: <path d="M12 4.5 20.5 19h-17zM12 10v4M12 16.5v.5" />,
  'sin-conexion': <path d="M4 9.5a12 12 0 0 1 4-2.3M11 6.6c3.3-.3 6.6.8 9 3M7 13a7 7 0 0 1 3-1.6M14.5 11.8a7 7 0 0 1 2.5 1.4M10 16.5a3 3 0 0 1 4 0M4 4l16 16" />,
  menu: <path d="M4.5 7h15M4.5 12h15M4.5 17h15" />,
  atras: <path d="M10 6.5 4.5 12l5.5 5.5M5 12h14.5" />,
  editar: <path d="M14.5 5.5l4 4L9 19H5v-4zM12.5 7.5l4 4" />,
  mas: <path d="M12 5v14M5 12h14" />,
  reloj: (<><circle cx="12" cy="12" r="7.5" /><path d="M12 8v4.5l3 2" /></>),
  arriba: <path d="M6.5 14.5 12 9l5.5 5.5" />,
  abajo: <path d="M6.5 9.5 12 15l5.5-5.5" />,
  ok: <path d="m5.5 12.5 4 4 9-9" />,
  llave: (<><circle cx="8.5" cy="12" r="3.5" /><path d="M12 12h8M17 12v3M20 12v2" /></>),
  tienda: <path d="M4.5 9.5 6 4.5h12l1.5 5M4.5 9.5h15M4.5 9.5c0 1.4 1.3 2.5 2.75 2.5S10 10.9 10 9.5M10 9.5c0 1.4 1.1 2.5 2 2.5s2-1.1 2-2.5M14 9.5c0 1.4 1.3 2.5 2.75 2.5S19.5 10.9 19.5 9.5M6 12v7.5h12V12" />,
} as const;

export type NombreIcono = keyof typeof TRAZOS;
export const NOMBRES_ICONO = Object.keys(TRAZOS) as NombreIcono[];

type Props = Omit<SVGProps<SVGSVGElement>, 'name'> & {
  nombre: NombreIcono;
  /** Nombre accesible. Sin él, el icono es decorativo y se oculta a los lectores. */
  titulo?: string;
};

export function Icono({ nombre, titulo, className, ...resto }: Props) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className ? `icono ${className}` : 'icono'}
      aria-hidden={titulo ? undefined : true}
      role={titulo ? 'img' : undefined}
      focusable="false"
      {...resto}
    >
      {titulo ? <title>{titulo}</title> : null}
      {TRAZOS[nombre]}
    </svg>
  );
}
