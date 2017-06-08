package antplutomigrator.correctness.utils;

import antplutomigrator.runner.AntMigrator;

import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class MigrateAntToPlutoTask extends TestTask {
    private final File buildFile;
    private final File outDir;
    private final String pkg;

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
    }

    @Override
    public String getDescription() {
        return "Migrating " + buildFile;
    }

    @Override
    public void execute() throws Exception {
        AntMigrator.main(new String[] {"-bf", buildFile.getAbsolutePath(), "-noFD", "-pkg", pkg, "-od", outDir.getAbsolutePath(), "-m"});
    }
}
