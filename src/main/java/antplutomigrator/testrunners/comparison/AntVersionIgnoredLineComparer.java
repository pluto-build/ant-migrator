package antplutomigrator.testrunners.comparison;

import java.io.File;

public class AntVersionIgnoredLineComparer implements LineComparer {
    @Override
    public boolean linesAreEqual(File f1, File f2, String l1, String l2) throws Exception {
        if (!f1.getName().endsWith("MANIFEST.MF") || !f2.getName().endsWith("MANIFEST.MF"))
            return false;

        if (l1.startsWith("Ant-Version: Apache Ant "))
            return true;

        return false;
    }
}
