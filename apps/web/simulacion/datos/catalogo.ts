import type { CancionSim } from '../tipos';

/**
 * Catálogo de la simulación: lo que «YouTube» devuelve al buscar. Canciones y artistas
 * reales; duraciones aproximadas (las de prototipos/herramientas/datos-de-ejemplo.md donde
 * las hay). Los `video_id` empiezan por `sim-`: **no son IDs de YouTube** y la pantalla del
 * bar usa el reproductor simulado con ellos (los IDs verificados están pendientes, doc 04).
 */
type Fila = [id: string, titulo: string, artista: string, duracion: string, genero: string];

const seg = (mmss: string) => mmss.split(':').map(Number).reduce((total, parte) => total * 60 + parte, 0);

const FILAS: Fila[] = [
  // La noche de los prototipos
  ['rebelion', 'Rebelión', 'Joe Arroyo', '5:58', 'salsa'],
  ['hips-dont-lie', "Hips Don't Lie", 'Shakira', '3:38', 'pop'],
  ['cali-pachanguero', 'Cali Pachanguero', 'Grupo Niche', '5:52', 'salsa'],
  ['dtmf', 'DtMF', 'Bad Bunny', '3:57', 'urbano'],
  ['monaco', 'Monaco', 'Bad Bunny', '4:27', 'urbano'],
  ['la-bicicleta', 'La Bicicleta', 'Carlos Vives y Shakira', '3:47', 'tropical'],
  ['nuevayol', 'NUEVAYoL', 'Bad Bunny', '3:03', 'urbano'],
  ['tqg', 'TQG', 'Karol G y Shakira', '3:17', 'urbano'],
  ['luna', 'Luna', 'Feid y ATL Jacob', '3:16', 'urbano'],
  ['una-aventura', 'Una aventura', 'Grupo Niche', '4:45', 'salsa'],
  ['titi-me-pregunto', 'Tití Me Preguntó', 'Bad Bunny', '4:03', 'urbano'],
  ['gasolina', 'Gasolina', 'Daddy Yankee', '3:12', 'urbano'],
  ['la-bachata', 'La Bachata', 'Manuel Turizo', '3:02', 'urbano'],
  ['despecha', 'Despechá', 'Rosalía', '2:37', 'pop'],
  // Más resultados para buscar
  ['baile-inolvidable', 'BAILE INoLVIDABLE', 'Bad Bunny', '6:07', 'urbano'],
  ['me-porto-bonito', 'Me Porto Bonito', 'Bad Bunny y Chencho Corleone', '2:58', 'urbano'],
  ['ojitos-lindos', 'Ojitos Lindos', 'Bad Bunny y Bomba Estéreo', '4:18', 'urbano'],
  ['provenza', 'PROVENZA', 'Karol G', '3:30', 'urbano'],
  ['classy-101', 'CLASSY 101', 'Feid y Young Miko', '3:15', 'urbano'],
  // Salsa y tropical de la casa (los que no estaban arriba)
  ['en-barranquilla-me-quedo', 'En Barranquilla me quedo', 'Joe Arroyo', '4:55', 'salsa'],
  ['la-noche', 'La noche', 'Joe Arroyo', '4:28', 'salsa'],
  ['gotas-de-lluvia', 'Gotas de lluvia', 'Grupo Niche', '5:12', 'salsa'],
  ['sin-sentimiento', 'Sin sentimiento', 'Grupo Niche', '5:20', 'salsa'],
  ['buenaventura-y-caney', 'Buenaventura y Caney', 'Grupo Niche', '5:05', 'salsa'],
  ['cali-aji', 'Cali ají', 'Grupo Niche', '4:30', 'salsa'],
  ['hagamos-lo-que-diga', 'Hagamos lo que diga el corazón', 'Grupo Niche', '5:00', 'salsa'],
  ['oiga-mire-vea', 'Oiga, mire, vea', 'Guayacán Orquesta', '4:40', 'salsa'],
  ['vas-a-llorar-por-mi', 'Vas a llorar por mí', 'Guayacán Orquesta', '4:50', 'salsa'],
  ['el-preso', 'El preso', 'Fruko y sus Tesos', '4:24', 'salsa'],
  ['los-charcos', 'Los charcos', 'Fruko y sus Tesos', '4:30', 'salsa'],
  ['periodico-de-ayer', 'Periódico de ayer', 'Héctor Lavoe', '6:20', 'salsa'],
  ['mi-gente', 'Mi gente', 'Héctor Lavoe', '5:55', 'salsa'],
  ['juanito-alimana', 'Juanito Alimaña', 'Héctor Lavoe', '5:45', 'salsa'],
  ['aguanile', 'Aguanile', 'Héctor Lavoe y Willie Colón', '7:30', 'salsa'],
  ['idilio', 'Idilio', 'Willie Colón', '5:35', 'salsa'],
  ['pedro-navaja', 'Pedro Navaja', 'Rubén Blades', '7:20', 'salsa'],
  ['plastico', 'Plástico', 'Rubén Blades y Willie Colón', '6:50', 'salsa'],
  ['decisiones', 'Decisiones', 'Rubén Blades', '4:25', 'salsa'],
  ['la-vida-es-un-carnaval', 'La vida es un carnaval', 'Celia Cruz', '4:35', 'salsa'],
  ['quimbara', 'Quimbara', 'Celia Cruz', '4:10', 'salsa'],
  ['usted-abuso', 'Usted abusó', 'Celia Cruz', '4:00', 'salsa'],
  ['lloraras', 'Llorarás', "Oscar D'León", '5:00', 'salsa'],
  ['conciencia', 'Conciencia', 'Gilberto Santa Rosa', '5:16', 'salsa'],
  ['que-alguien-me-diga', 'Que alguien me diga', 'Gilberto Santa Rosa', '4:50', 'salsa'],
  ['vivir-mi-vida', 'Vivir mi vida', 'Marc Anthony', '4:12', 'salsa'],
  ['valio-la-pena', 'Valió la pena', 'Marc Anthony', '4:50', 'salsa'],
  ['tu-amor-me-hace-bien', 'Tu amor me hace bien', 'Marc Anthony', '4:40', 'salsa'],
  ['desnudate-mujer', 'Desnúdate mujer', 'Frankie Ruiz', '4:40', 'salsa'],
  ['la-cura', 'La cura', 'Frankie Ruiz', '4:30', 'salsa'],
  ['tu-me-quemas', 'Tú me quemas', 'Eddie Santiago', '4:35', 'salsa'],
  ['amores-como-el-nuestro', 'Amores como el nuestro', 'Jerry Rivera', '4:20', 'salsa'],
  ['un-verano-en-nueva-york', 'Un verano en Nueva York', 'El Gran Combo de Puerto Rico', '5:05', 'salsa'],
  ['brujeria', 'Brujería', 'El Gran Combo de Puerto Rico', '4:50', 'salsa'],
  ['fuego-en-el-23', 'Fuego en el 23', 'Sonora Ponceña', '4:45', 'salsa'],
  ['micaela', 'Micaela', 'Sonora Carruseles', '4:30', 'salsa'],
  ['la-pollera-colora', 'La pollera colorá', 'Pedro Salcedo y su orquesta', '3:00', 'tropical'],
  ['colombia-tierra-querida', 'Colombia tierra querida', 'Lucho Bermúdez', '3:00', 'tropical'],
  ['la-gota-fria', 'La gota fría', 'Carlos Vives', '4:45', 'vallenato'],
];

export const CATALOGO: Record<string, CancionSim> = Object.fromEntries(
  FILAS.map(([id, titulo, artista, duracion, genero]) => [
    `sim-${id}`,
    { video_id: `sim-${id}`, titulo, artista, duracion_seg: seg(duracion), embebible: true, disponibilidad: 'disponible', genero } satisfies CancionSim,
  ]),
);

// Un mix largo, como el de los prototipos: no se puede pedir porque dura más de 10 min (RN-06).
CATALOGO['sim-mix-bad-bunny-2025'] = {
  video_id: 'sim-mix-bad-bunny-2025',
  titulo: 'Mix Bad Bunny 2025',
  artista: 'Grandes éxitos',
  duracion_seg: seg('1:12:40'),
  embebible: true,
  disponibilidad: 'disponible',
  genero: 'urbano',
};

/** Las 42 de «Salsa y tropical de la casa», en orden. */
export const LISTA_SALSA_Y_TROPICAL = [
  'rebelion', 'en-barranquilla-me-quedo', 'la-noche', 'gotas-de-lluvia', 'sin-sentimiento', 'buenaventura-y-caney',
  'cali-pachanguero', 'una-aventura', 'oiga-mire-vea', 'vas-a-llorar-por-mi', 'el-preso', 'los-charcos',
  'periodico-de-ayer', 'mi-gente', 'idilio', 'pedro-navaja', 'plastico', 'decisiones', 'la-vida-es-un-carnaval',
  'quimbara', 'usted-abuso', 'lloraras', 'conciencia', 'que-alguien-me-diga', 'vivir-mi-vida', 'valio-la-pena',
  'desnudate-mujer', 'la-cura', 'tu-me-quemas', 'amores-como-el-nuestro', 'un-verano-en-nueva-york', 'brujeria',
  'fuego-en-el-23', 'la-pollera-colora', 'colombia-tierra-querida', 'la-gota-fria', 'micaela', 'juanito-alimana',
  'aguanile', 'cali-aji', 'hagamos-lo-que-diga', 'tu-amor-me-hace-bien',
].map((id) => `sim-${id}`);

/** Una segunda lista, inactiva, con el nombre de ejemplo del diccionario de datos. */
export const LISTA_VIERNES_CROSSOVER = ['hips-dont-lie', 'la-bicicleta', 'tqg', 'provenza', 'me-porto-bonito', 'ojitos-lindos', 'la-gota-fria', 'vivir-mi-vida'].map(
  (id) => `sim-${id}`,
);
