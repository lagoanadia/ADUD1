# AD-UD1-A2 · Importador de un CSV real

Programa que importa `clientes_importacion.csv` (una exportación real y bastante
maltratada de otro sistema de gestión), separando las filas válidas de las que no
lo son sin detener la importación por una fila suelta, y mostrando al final un
resumen agrupado por motivo de rechazo.

## Cómo se ejecuta

Con Maven (`exec-maven-plugin`):

```bash
mvn -q compile exec:java
```

Por defecto lee `clientes_importacion.csv` del directorio del proyecto. Para usar
otro fichero:

```bash
mvn -q compile exec:java -Dexec.args="otra_ruta.csv"
```

El programa crea `salida/` si no existe, y escribe ahí `clientes_ok.csv`
(los clientes válidos) y `rechazados.csv` (línea, contenido original y motivo
de cada fila descartada).

## 1 · Observaciones del fichero (paso 2)

Antes de escribir una sola línea de código se examinó el fichero con las
herramientas que pide el enunciado:

```bash
$ file --mime clientes_importacion.csv
clientes_importacion.csv: text/csv; charset=utf-8

$ wc -l clientes_importacion.csv
195 clientes_importacion.csv

$ head -3 clientes_importacion.csv
DNI,NOMBRE,TELEFONO,CP,
10000037M,Luis Vázquez,643464097,36405,
10000074L,María Núñez,687366946,15075,

$ grep -c $'\r' clientes_importacion.csv
195
```

De aquí salieron ya varias sorpresas respecto al enunciado y a las "piezas" de
ejemplo (que asumen `;` como separador):

- **El separador real es la coma**, no `;`. El fichero es un CSV "de toda la
  vida", no el formato con `;` que sugiere la tabla de piezas. Hubo que usar
  `split(",", -1)` y no `split(";", -1)`.
- **Cada línea termina en una coma de más** (incluida la cabecera:
  `DNI,NOMBRE,TELEFONO,CP,`). Eso significa que `split(",", -1)` siempre deja un
  quinto campo vacío al final, que hay que descartar antes de exigir "4 campos".
  Si no se hace, **todas** las filas, incluso las correctas, se rechazarían por
  "campos de más".
- **Saltos de línea `CRLF`** (`grep -c $'\r'` da 195, una por línea): el fichero
  se generó o se tocó en Windows. No obliga a cambiar nada porque
  `BufferedReader.readLine()` ya quita `\r` y `\n` solo, pero conviene saberlo
  para no sorprenderse si se abre con herramientas que no lo gestionan.
- **El fichero es UTF-8 real** (lo confirma `file`, no es una suposición), así
  que se lee con `StandardCharsets.UTF_8` y no con `ISO_8859_1` pese a que el
  ejemplo genérico de las piezas usa esa constante.
- Pasada la línea 142 (ver más abajo) el texto sale con secuencias como
  `XoÃ¡n Ferreiro` en vez de `Xoán Ferreiro`. **No es un error de lectura**: son
  bytes UTF-8 correctos, pero el *contenido* se codificó a UTF-8 **dos veces**
  en algún paso de una exportación posterior (el síntoma exacto de "el tramo
  final sale mal" que avisa el enunciado). Arreglarlo del todo exige detectar
  la codificación línea a línea, que es la ampliación opcional de +1 punto y
  no se ha implementado aquí; se documenta como limitación conocida y esas
  filas se siguen importando si el resto de campos es válido (el nombre sale
  "sucio" visualmente, pero el cliente existe y tiene un DNI y un CP correctos).
- Hay una **línea de comentario del exportador** incrustada a mitad de fichero:
  `#  exportado por GestTaller 4.2 el 12/03/2019,,,,` (línea 142). No es un
  cliente y hay que reconocerla y descartarla explícitamente.
- `grep -c ';;'` no tiene sentido aquí (no hay `;` en el fichero); el
  equivalente real para "campos vacíos" es mirar los dos `,,` seguidos, que
  aparecen en filas con el nombre o el CP en blanco.

## 2 · Criterios de validez (paso 4)

Una fila se considera **válida** si, tras quitar la coma final de relleno,
cumple todo esto, en este orden (la primera condición que falla es el único
motivo que se anota — ver la pregunta de autocomprobación sobre filas con más
de un problema):

1. **Número de campos**: exactamente 4 después de descartar el quinto campo
   vacío de relleno.
2. **DNI**: `\d{8}[A-Za-z]` (ocho cifras y una letra). No se comprueba la letra
   de control real (eso es CE1.1 de otra asignatura), solo el formato.
3. **Nombre**: no puede estar vacío.
4. **Código postal**: exactamente 5 cifras.
5. **Teléfono**: nueve cifras, con o sin espacios y con prefijo `+34` opcional
   (para aceptar tanto `643464097` como `+34 600 12 34 56`).
6. **DNI no repetido**: si un DNI ya se importó antes en el mismo fichero, la
   fila se rechaza como duplicado.

Los cuatro primeros son los que pide literalmente el enunciado (campos, DNI,
CP, vacíos). El de teléfono y el de duplicados **se han añadido** porque el
fichero trae casos claramente intencionados que ninguno de los cuatro
criterios anteriores detecta por sí solo:

- Un teléfono con el valor `no disponible` (línea 96): no es un número, así
  que una fila "sin más validación que no esté vacía" lo dejaría pasar.
- Un mismo DNI (`10000444K`) aparece en la línea 14 con un cliente y otra vez
  en la línea 113 con otro nombre y teléfono distintos (línea 113).

El código postal merece una nota aparte: **28 de las 37 filas rechazadas** lo
son por tener solo 4 cifras (por ejemplo `8597` en vez de `08597`). Son
siempre códigos que, con un cero delante, serían perfectamente válidos
(provincia de Barcelona). Es un patrón demasiado repetido para ser casualidad:
lo más probable es que el sistema de origen guardase el CP como número en
algún momento y perdiera el cero inicial. **Se ha decidido no "arreglarlo"
automáticamente** (añadiendo el cero a ciegas) porque el enunciado exige
explícitamente 5 cifras y porque adivinar el código postal real de un cliente
sin confirmarlo es más arriesgado que marcarlo para revisión humana — se
rechaza con el motivo `código postal no son 5 cifras` y queda documentado
aquí para quien revise `rechazados.csv`.

Dos casos más que valía la pena anotar aunque no cambien el resultado:

- La línea 39, `"Talleres Miño, S.L."`, trae una coma **dentro** de un campo
  entrecomillado al estilo CSV estándar. Un `split(",", -1)` ingenuo no respeta
  las comillas, así que esa coma se cuenta como separador y la fila sale con 5
  campos en vez de 4: se rechaza por "campos de más". Tratarla bien exigiría un
  parser de CSV real (con comillas), fuera del alcance de esta unidad; se
  documenta como limitación conocida en vez de ignorarla en silencio.
- La línea 48, `"Talleres ""O Rápido"""`, también usa comillas (con comillas
  dobles escapadas dentro), pero como no tiene ninguna coma interna, el campo
  cuenta como uno solo y la fila sí se importa — con las comillas literales
  todavía dentro del nombre, porque tampoco se hace "des-entrecomillado" de
  CSV. Se deja así a propósito, documentado, en vez de añadir una limpieza a
  medias que solo cubriera este caso concreto.

## 3 · Resumen de la ejecución (paso 7)

```
Líneas leídas: 196
Importadas: 158
Rechazadas: 37

Rechazos por motivo:
  código postal no son 5 cifras                           28
  nombre vacío                                             1
  esperaba 4 campos, hay 5                                 2
  DNI con formato incorrecto                               3
  teléfono no es un número válido                          1
  DNI duplicado                                             1
  línea de comentario del exportador, no es un cliente     1
```

"Líneas leídas" cuenta también la cabecera (196 = 1 cabecera + 195 filas de
datos); 195 = importadas + rechazadas (158 + 37).

## 4 · Fallos de fichero (paso 6)

Si `clientes_importacion.csv` no existe, el programa no intenta leer nada:
imprime qué ruta absoluta buscó y termina con código de salida 1. Si falla la
creación de `salida/`, o un error de E/S interrumpe la lectura o la escritura
a mitad (permisos, disco lleno...), se captura por tipo de excepción
(`NoSuchFileException`, `AccessDeniedException`, `IOException` genérica, de
la más concreta a la más general) y se informa de qué fichero y por qué,
también con salida 1. Ninguno de estos casos tiene que ver con una fila
suelta: una fila mala nunca detiene el proceso, solo pasa a `rechazados.csv`.

## 5 · Forma de acceso (paso 8)

Este programa usa acceso secuencial: se lee el fichero de principio a fin con
`BufferedReader.readLine()`, sin saltar nunca hacia atrás ni hacia una
posición conocida de antemano. Es la forma adecuada porque la tarea en sí es
secuencial por naturaleza: para importar hay que examinar **todas y cada una**
de las filas, en cualquier orden, así que no existe ninguna fila a la que
convenga "ir directamente" sin pasar por las demás. El coste es lineal,
O(n), se mire como se mire.

Con acceso aleatorio (por ejemplo, un fichero de registros de longitud fija
con `RandomAccessFile`, o un índice por DNI) ganaríamos la capacidad de saltar
directamente a un cliente conocido sin leer el resto, algo muy valioso si
luego quisiéramos *consultar* o *actualizar* un cliente concreto con
frecuencia. Pero aquí perderíamos justo lo que nos sobra en una importación
puntual: simplicidad, y la posibilidad de usar formatos de texto libres (CSV
con campos de longitud variable) que el acceso aleatorio por posición de
byte no soporta bien sin añadir una capa de índices.

Dentro de este mismo programa sí habría un caso donde el acceso aleatorio
compensaría: si en vez de importar una vez al mes hubiera que buscar y
actualizar clientes concretos todo el día (por ejemplo, para cambiar un
teléfono dado un DNI), cargar y recorrer linealmente 200 000 líneas cada vez
sería absurdo; ahí lo natural ya no es un CSV recorrido secuencialmente, sino
una base de datos con índice por DNI (acceso prácticamente directo), que es
justo el problema que resuelve el resto de la asignatura.

## 6 · Diseño

- **`Path` / `Files`, sin `java.io.File`**: igual que en la actividad 1, toda
  la E/S usa `java.nio.file`.
- **Charset explícito en los tres recursos** (`Files.newBufferedReader` /
  `newBufferedWriter`), nunca el `Charset` por defecto de la plataforma.
- **`try-with-resources` con los tres recursos a la vez** (un lector, dos
  escritores): si falla a mitad, los tres se cierran igualmente.
- **`validar()` devuelve `Optional<String>`** con el motivo en vez de un
  `boolean`, para no repetir la lógica de validación al construir el mensaje
  de rechazo (la pista del enunciado).
- **El número de línea se cuenta en el propio bucle de lectura** (variable
  `leidas`), no se recalcula después: es lo que permite que
  `rechazados.csv` diga "línea 60" y no solo "DNI incorrecto".
- **`camposDeLaLinea()` centraliza el `split` y el descarte de la coma final
  de relleno**, porque se usa dos veces (en `validar()` y en `aCliente()`) y
  tiene que comportarse igual en ambos sitios.
- **`Map<String, Integer>` con `merge(motivo, 1, Integer::sum)`** para el
  resumen agrupado por motivo, tal y como sugiere la pista del paso 7; se
  usa `LinkedHashMap` para que el orden de salida sea el de aparición, no
  alfabético.
- **Contenido entrecomillado en `rechazados.csv`**: el campo "contenido" se
  envuelve en `"..."` y las comillas internas se duplican (`"` → `""`), el
  escapado estándar de CSV, para que una fila rechazada que ya traía `;` o
  `"` (como la de "Talleres Miño") no rompa el propio fichero de salida.
