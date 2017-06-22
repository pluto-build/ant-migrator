package antplutomigrator.correctness.comparison;

import java.io.File;

/**
 * Created by manuel on 22.06.17.
 */
public class EqualLineComparer implements LineComparer {
    @Override
    public boolean linesAreEqual(File f1, File f2, String l1, String l2) throws Exception {
        return l1.equals(l2);
    }
}
