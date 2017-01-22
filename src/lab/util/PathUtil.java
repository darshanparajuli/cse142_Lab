package lab.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtil {

    private PathUtil() {
        // prevent instantiation
    }

    public static String getLabPath() {
        try {
            Path path = Paths.get(PathUtil.class.getResource("").toURI());
            while (!path.endsWith("Lab")) {
                path = path.getParent();
            }
            return path.toString();
        } catch (URISyntaxException e) {
            return null;
        }

    }

}
