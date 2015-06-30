import sun.nio.ch.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Example for APP1, but can be extended to APP0 and more.
public class Main {

    public static void main(String[] args) throws IOException {

        // The following is an example that takes a JPEG and created a separate image using the embedded thumbnail.
        ThumbStream ts = new ThumbStream("test.jpg");
        final Path destination = Paths.get("test-thumb.jpg");
        Files.copy(ts, destination);
    }
}
