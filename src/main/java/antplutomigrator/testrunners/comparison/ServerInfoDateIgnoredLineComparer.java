package antplutomigrator.testrunners.comparison;

import java.io.File;

public class ServerInfoDateIgnoredLineComparer implements LineComparer {
    @Override
    public boolean linesAreEqual(File f1, File f2, String l1, String l2) throws Exception {
        if (!f1.getName().endsWith("ServerInfo.properties") || !f2.getName().endsWith("ServerInfo.properties"))
            return false;

        if (l1.startsWith("server.built="))
            return true;

        return false;
    }
}
