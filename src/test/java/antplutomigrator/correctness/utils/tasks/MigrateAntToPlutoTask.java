package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import antplutomigrator.runner.AntMigrator;

import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class MigrateAntToPlutoTask extends TestTask {
    private final File buildFile;
    private final File outDir;
    private final String pkg;
    private final boolean fileDepencencyDiscovery;

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = false;
    }

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg, boolean fileDepencencyDiscovery) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = fileDepencencyDiscovery;
    }

    @Override
    public String getDescription() {
        return "Migrating " + buildFile;
    }

    @Override
    public void execute() throws Exception {
        if (fileDepencencyDiscovery)
            AntMigrator.main(new String[] {"-bf", buildFile.getAbsolutePath()/*, "-noFD"*/, "-pkg", pkg, "-od", outDir.getAbsolutePath(), "-m"});
        else
            AntMigrator.main(new String[] {"-bf", buildFile.getAbsolutePath(), "-noFD", "-pkg", pkg, "-od", outDir.getAbsolutePath(), "-m"});
    }
}
