package adud1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.time.Instant;

/**
 * Explorador de directorios por línea de comandos basado en {@code java.nio.file}.
 * <p>
 * Recorre la ruta indicada por argumentos, mostrando para cada elemento sus
 * permisos ({@code rwx}), tamaño en bytes, fecha de última modificación y
 * nombre. Por defecto solo lista un nivel; con el modificador {@code -r}
 * recorre el árbol completo de forma recursiva, con sangría creciente según
 * la profundidad.
 * <p>
 * Un fallo de acceso en un elemento concreto (por ejemplo,
 * {@link java.nio.file.AccessDeniedException}) se trata donde ocurre y no
 * detiene el recorrido: se cuenta en el resumen final y se sigue con el
 * resto de elementos.
 */
public class Explorador {
    private static final DateTimeFormatter FECHA = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static int ficheros, directorios, sinAcceso; // (8)
    private static long bytes; // (8)
    private static int codigo = 0;

    /**
     * Punto de entrada del programa.
     * <p>
     * Interpreta los argumentos de la línea de comandos (una ruta opcional y
     * el modificador {@code -r}), valida que la ruta exista, y lanza el
     * listado de un nivel o el recorrido recursivo según corresponda.
     * Termina imprimiendo un resumen con el número de ficheros, directorios,
     * bytes totales y elementos sin acceso.
     *
     * @param args argumentos de la línea de comandos: una ruta (si se omite,
     *             se usa el directorio actual) y, opcionalmente, {@code -r}
     *             para recorrer el árbol de forma recursiva. El orden entre
     *             ambos no importa.
     */
    public static void main(String[] args) {
        // ejecuta con java Explorador por la terminal !

        // TODO (2) leer ruta y opción -r; construir raiz con Path.of

        String txtRuta = ".";
        boolean recursivo = false;

        for (String a : args) // recorre cada objeto de los argumentos introducidos por terminal
        {
            if (a.equals("-r")) {
                recursivo = true;
                // flag por si encuentra "-r" por ejemplo: java Explorador -r
            } else {
                txtRuta = a;
                // guardamos todo lo que no sea "-r" como ruta
                System.out.println(a);
            }
        }
        Path root = Path.of(txtRuta).toAbsolutePath().normalize();
        // convertimos el string ya identificado como nuestra ruta en un objeto Path
        // toAbsolutePath completa la ruta desde la raiz del disco es decir añade C:\
        // el metodo normalize elimina . .. para obtener la ruta completa limpia
        System.out.println("Explorando " + root);

    // TODO (3) validar: no existe → exit 1; es fichero → una línea y fin

        if (!Files.exists(root)) {
            System.out.print("No existe");
            System.exit(1);
            // comprobamos si la ruta existe en el disco
        } else {
            // si existe en el disco comprobamos si es un directorio

            if (!Files.isDirectory(root)) {
                System.out.println(linea(root, root));

                return;
            }
        }
       // TODO (4)(6) try-with-resources con Files.list o Files.walk

        if (recursivo) {
            try (Stream<Path> hijos = Files.walk(root).sorted().skip(1)) {
                hijos.forEach(hijo -> System.out.println(linea(hijo, root)));
            } catch (NoSuchFileException e) { // la ruta desapareció
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (NotDirectoryException e) { // no se puede listar un fichero
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (AccessDeniedException e) { // sin permiso para la raíz
                System.err.println("ERROR: " + e.getMessage());
                sinAcceso++;
                codigo = 1;
            } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (IOException e) { // cualquier otro fallo de E/S
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            }
        } else {
            try (Stream<Path> hijos = Files.list(root).sorted();)
            // try () se cierra solo.
            {
                hijos.forEach(hijo -> System.out.println(linea(hijo, root)));
                // bucle for each para un stream<path> !

            } catch (NoSuchFileException e) { // la ruta desapareció
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (NotDirectoryException e) { // no se puede listar un fichero
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (AccessDeniedException e) { // sin permiso para la raíz
                System.err.println("ERROR: " + e.getMessage());
                sinAcceso++;
                codigo = 1;
            } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (IOException e) { // cualquier otro fallo de E/S
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            }

        }

        // TODO (7) catch de las excepciones, de la más concreta a la más general
        // TODO (8) imprimir el resumen y System.exit(codigo)
        System.out.printf("%n%d ficheros · %d directorios · %d bytes · %d sin acceso%n",
                ficheros, directorios, bytes, sinAcceso);
        System.exit(codigo);
    }

    /**
     * Construye la línea de salida de un elemento del árbol: permisos,
     * tamaño, fecha de última modificación y nombre, con sangría según la
     * profundidad respecto a {@code base}.
     * <p>
     * Incrementa los contadores estáticos del resumen final
     * ({@code ficheros}/{@code directorios}/{@code bytes}/{@code sinAcceso})
     * según corresponda. Si falla la lectura del tamaño o de la fecha, el
     * fallo se captura aquí mismo (no se propaga, porque este método se
     * invoca desde dentro de un {@code forEach}, que no admite excepciones
     * comprobadas) y se muestra el motivo en lugar del dato que no se pudo
     * leer.
     *
     * @param p    la ruta del elemento a describir.
     * @param base la raíz desde la que se está explorando, usada para
     *             calcular el nivel de profundidad y, por tanto, la sangría.
     * @return la línea formateada lista para imprimir.
     */
    static String linea(Path p, Path base) {
        // TODO (5) permisos rwx, tamaño y fecha
        String permisos = (Files.isReadable(p) ? "r" : "-") + (Files.isWritable(p) ? "w" : "-")
                + (Files.isExecutable(p) ? "x" : "-");
        boolean esDirectorio = Files.isDirectory(p);
        if (esDirectorio) {
            directorios++;
        } else {
            ficheros++;
        }
        long sizeLong;
        String sizeString = "";

        FileTime timeNoFormat;
        Instant aux;
        String time;

        try {
            sizeLong = Files.size(p);
            sizeString = Long.toString(sizeLong);
            bytes += sizeLong;
        } catch (NoSuchFileException e) { // la ruta desapareció
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (NotDirectoryException e) { // no se puede listar un fichero
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (AccessDeniedException e) { // sin permiso para la raíz
            sizeString = "ERROR: " + e.getMessage();
            sinAcceso++;
            codigo = 1;
        } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (IOException e) { // cualquier otro fallo de E/S
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        }

        try {
            timeNoFormat = Files.getLastModifiedTime(p);
            aux = timeNoFormat.toInstant();
            time = FECHA.format(aux);
        } catch (NoSuchFileException e) { // la ruta desapareció
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (NotDirectoryException e) { // no se puede listar un fichero
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (AccessDeniedException e) { // sin permiso para la raíz
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (IOException e) { // cualquier otro fallo de E/S
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        }

        // TODO (6) sangría según el nivel respecto a base
        int nivel = base.relativize(p).getNameCount();
        String sangria = " ".repeat(nivel - 1);

        // TODO (7) si falla la lectura de atributos, devolver la ruta con el motivo
        String nombre = p.getFileName() + (esDirectorio ? "/" : "");
        String datos = String.format("%s%-15s %5s  %s  %s", sangria, permisos, sizeString, time, nombre);
        return datos;
    }
}