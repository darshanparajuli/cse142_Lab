package crux;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ParserTest {

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameter(1)
    public String expectedPath;

    @Parameterized.Parameter(2)
    public String name;

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() throws Exception {
        final List<Object[]> list = new ArrayList<>();

        final Path path = Paths.get(ParserTest.class.getResource("test_files").toURI());
        Files.list(path)
                .filter(p -> p.toString().endsWith(".crx"))
                .forEach(p -> {
                    final String src = p.toString();
                    final String expected = src.replace(".crx", ".out");
                    final String name = p.getFileName().toString().replace(".crx", "");
                    final Object[] item = {src, expected, name};
                    list.add(item);
                });

        return list;
    }

    @Test
    public void testScanner() throws Exception {
        final Path path = Paths.get(expectedPath);
        final Scanner scanner = new Scanner(new FileReader(this.path));
        final Parser parser = new Parser(scanner);
        parser.parse();

        final List<String> actual = new ArrayList<>();
        final List<String> expected = Files.readAllLines(path);

        if (parser.hasError()) {
            actual.add("Error parsing file.");
            actual.addAll(Arrays.asList(parser.errorReport().split("\n")));
        } else {
            actual.addAll(Arrays.asList(parser.parseTreeReport().split("\n")));
        }

        for (int i = actual.size() - 1; i >= 0; i--) {
            if (actual.get(i).isEmpty()) {
                actual.remove(i);
                break;
            }
        }

        for (int i = expected.size() - 1; i >= 0; i--) {
            if (expected.get(i).isEmpty()) {
                expected.remove(i);
                break;
            }
        }

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }
}