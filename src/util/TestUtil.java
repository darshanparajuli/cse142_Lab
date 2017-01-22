package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestUtil {

    private TestUtil() {
        // prevent instantiation
    }

    public static void compareToFile(String actual, Path file, Comparator<Character> comparator) throws IOException {
        final BufferedReader reader = Files.newBufferedReader(file);

        for (int c = reader.read(), i = 0; c != -1; c = reader.read(), i++) {
            comparator.compare(actual.charAt(i), (char) c);
        }

        reader.close();
    }

    public interface Comparator<T> {

        void compare(T actual, T expected);
    }
}
