package adud1;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class Explorador
{
    private static final DateTimeFormatter FECHA = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static int ficheros, directorios, sinAcceso; // (8)
    private static long bytes; // (8)
    public static void main(String[] args) 
    {
        String txtRuta = ".";
        boolean recursivo = false;
        for(String a: args)
        {
            if(a.equals("-r"))
            {
                System.out.print("Recursivo SI    ");
                recursivo = true;
            }
            else 
            {
                txtRuta = a;
             
                System.out.println(a);
            }
        }
        Path root = Path.of(txtRuta).toAbsolutePath().normalize();
        System.out.println(root);

        
        {
            //Path p = Paths.get(arg);
           // explorar(p, p);
        }
        if(!Files.exists(root))
        {
            System.exit(1);
        }
        else 
        {
            if(!Files.isDirectory(root))
            {
                System.out.println(linea(root, root));
                return;
            }
        }

        
        // TODO (4)(6) try-with-resources con Files.list o Files.walk
        // TODO (7) catch de las excepciones, de la más concreta a la más general
        // TODO (8) imprimir el resumen y System.exit(codigo)
    }
    static String linea(Path p, Path base)
    {
        // TODO (5) permisos rwx, tamaño y fecha
        // TODO (6) sangría según el nivel respecto a base
        // TODO (7) si falla la lectura de atributos, devolver la ruta con el motivo
            return "";
    }
}