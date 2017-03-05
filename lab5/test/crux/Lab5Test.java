package crux;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import types.TypeChecker;
import util.TestUtil;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class Lab5Test {

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameter(1)
    public String expectedPath;

    @Parameterized.Parameter(2)
    public String name;

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() throws Exception {
        final List<Object[]> list = new ArrayList<>();

        final Path path = Paths.get(Lab5Test.class.getResource("test_files").toURI());
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
    public void testParser() throws Exception {
        final Path path = Paths.get(expectedPath);
        final String actualResult = runParser(path.toString());
        TestUtil.compareToFile(actualResult, path, ((actual, expected) -> Assert.assertEquals(expected, actual)));
    }

    private String runParser(String sourceFilename) throws Exception {
        final Scanner s = new Scanner(new FileReader(path));
        final Parser p = new Parser(s);
        ast.Command syntaxTree = p.parse();
        if (p.hasError()) {
            return "Error parsing file " +
                    sourceFilename +
                    "\n" +
                    p.errorReport() +
                    "\n";
        }

        TypeChecker tc = new TypeChecker();
        tc.check(syntaxTree);
        if (tc.hasError()) {
            return "Error type-checking file.\n" +
                    tc.errorReport() + "\n";
        }

        return "Crux Program is has no type errors.\n";
    }
}