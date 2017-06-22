package antplutomigrator.correctness.comparison;

import java.io.File;

/**
 * Created by manuel on 02.06.17.
 */
public interface FileComparer {
    boolean filesAreEqual(File f1, File f2);
}
