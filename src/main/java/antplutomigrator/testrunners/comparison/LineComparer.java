package antplutomigrator.testrunners.comparison;

import java.io.File;

/**
 * Created by manuel on 21.06.17.
 */
public interface LineComparer {
    boolean linesAreEqual(File f1, File f2, String l1, String l2) throws Exception;
}
