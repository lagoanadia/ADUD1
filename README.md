# AD-UD1-A1 · Explorador de directorios con java.nio.file

Herramienta de línea de comandos que recorre un directorio (un nivel o recursivo, con `-r`), mostrando permisos (`rwx`), tamaño, fecha de última modificación y nombre de cada elemento. Trata cada fallo de acceso donde ocurre, sin detener el recorrido, y muestra un resumen final.

## Cómo se ejecuta

Con Maven (`exec-maven-plugin`):

```bash
mvn -q compile exec:java -Dexec.args="<ruta> [-r]"
```

Ejemplos:

```bash
mvn -q compile exec:java -Dexec.args="prueba"
mvn -q compile exec:java -Dexec.args="prueba -r"
```

Si no se indica ninguna ruta, se explora el directorio actual.

## Pruebas realizadas

Carpeta de prueba usada, creada así:

```bash
mkdir -p prueba/docs/sub prueba/secreta
echo hola > prueba/a.txt
echo "mas texto" > prueba/docs/b.md
echo x > prueba/docs/sub/c.txt
chmod 000 prueba/secreta
```

### 1. Un nivel (sin `-r`)

```
$ java adud1.Explorador prueba
prueba
rwx 5 bytes 2026-09-17 11:39 a.txt
rwx 0 bytes 2026-09-17 11:39 docs
rwx 0 bytes 2026-09-17 11:38 secreta

3 ficheros · 2 directorios · 5 bytes · 0 sin acceso
```

Solo muestra el contenido directo de `prueba`, sin entrar en `docs`.

### 2. Recursivo (`-r`)

```
$ java adud1.Explorador prueba -r
prueba
rwx 5 bytes 2026-09-17 11:39 a.txt
rwx 0 bytes 2026-09-17 11:39 docs
 rwx 10 bytes 2026-09-17 11:39 b.md
 rwx 0 bytes 2026-09-17 09:50 sub
  rwx 2 bytes 2026-09-17 11:39 c.txt
rwx 0 bytes 2026-09-17 11:38 secreta

4 ficheros · 3 directorios · 17 bytes · 0 sin acceso
```

Baja por todo el árbol, con sangría creciente según la profundidad (`docs` es nivel 1, `sub` nivel 2, `c.txt` nivel 3).

**Sobre `secreta` (chmod 000):** en este entorno (Windows con Git Bash, ejecutando con un usuario con privilegios elevados) el `chmod 000` no llega a bloquear el acceso real a nivel de sistema de ficheros, así que el programa sigue leyendo `secreta` con normalidad en vez de mostrar un fallo de `AccessDeniedException`. El propio enunciado de la práctica avisa de este comportamiento en sistemas Windows/administrador. El código sí contempla y captura `AccessDeniedException` (contándolo en `sinAcceso`) para cuando se ejecute en un sistema donde el permiso sí se respete (por ejemplo, un servidor Linux real, el escenario del contexto profesional de la práctica).

### 3. Ruta inexistente

```
$ java adud1.Explorador carpeta-que-no-existe
carpeta-que-no-existe
C:\Users\Nadia.PC-01\Desktop\carpeta-que-no-existe
No existe
```

Termina con código de salida 1, sin intentar recorrer nada.

### 4. Un fichero en vez de un directorio

```
$ java adud1.Explorador prueba/a.txt
prueba/a.txt
C:\Users\Nadia.PC-01\Desktop\prueba\a.txt
rwx 5 bytes 2026-09-17 11:39 a.txt
```

Muestra solo la línea de ese fichero y termina, sin intentar listar nada más.

## Diseño

- **`Path` y `Files`, sin `java.io.File`**: toda la validación, listado y lectura de atributos usa `java.nio.file`, que informa el motivo exacto de cada fallo (excepción con nombre y ruta) en vez de devolver `null`/`false` en silencio.
- **`try-with-resources`** en los dos recorridos (`Files.list`/`Files.walk`), porque el `Stream<Path>` que devuelven mantiene abierta la carpeta mientras se usa.
- **`linea(Path p, Path base)`** centraliza la lectura de atributos de cada elemento (permisos, tamaño, fecha, sangría por nivel) y captura sus propias excepciones, porque se invoca desde dentro de una lambda (`forEach`), que no admite propagar excepciones comprobadas hacia fuera.
- **Excepciones capturadas de la más concreta a la más general** (`NoSuchFileException`, `NotDirectoryException`, `AccessDeniedException`, `UncheckedIOException`, `IOException`), tanto en el recorrido principal como dentro de `linea()`.
- **Resumen final** con contadores `static` (`ficheros`, `directorios`, `bytes`, `sinAcceso`), incrementados dentro de `linea()` porque es el método que se ejecuta una vez por cada elemento procesado.

## Nivel de IA

IA-1: la IA (Claude) explicó los conceptos de la teoría (`Path`, `Files`, excepciones comprobadas/no comprobadas, `try-with-resources`) y señaló errores de compilación/lógica con preguntas guía, pero no escribió el código de la solución.
