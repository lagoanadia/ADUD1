package adud1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.time.Instant;

public class Explorador {
    private static final DateTimeFormatter FECHA = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static int ficheros, directorios, sinAcceso; // (8)
    private static long bytes; // (8)
    private static int codigo = 0;

    public static void main(String[] args) {
        // ejecuta con java Explorador por la terminal !

        // TODO (2) leer ruta y opción -r; construir raiz con Path.of

        String txtRuta = ".";
        boolean recursivo = false;

        for (String a : args) // recorre cada objeto de los argumentos introducidos por terminal
        {
            if (a.equals("-r")) {
                System.out.print("Recursivo SI    ");
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
        System.out.println(root);

        {
            // Path p = Paths.get(arg);
            // explorar(p, p);
        }
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
            try (Stream<Path> hijos = Files.walk(root).skip(1)) {
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

    static String linea(Path p, Path base) {
        // TODO (5) permisos rwx, tamaño y fecha
        String permisos = (Files.isReadable(p) ? "r" : "-") + (Files.isWritable(p) ? "w" : "-")
                + (Files.isExecutable(p) ? "x" : "-");
        if (Files.isDirectory(p)) {
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
        String datos = sangria + permisos + " " + sizeString + " bytes " + time + " " + p.getFileName();
        return datos;
    }
}