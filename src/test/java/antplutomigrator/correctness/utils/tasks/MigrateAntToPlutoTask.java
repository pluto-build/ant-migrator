package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import antplutomigrator.runner.AntMigrator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 08.06.17.
 */
public class MigrateAntToPlutoTask extends TestTask {
    private final File buildFile;
    private final File outDir;
    private final String pkg;
    private final boolean fileDepencencyDiscovery;
    private final boolean debug;

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = false;
        this.debug = false;
    }

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg, boolean fileDepencencyDiscovery, boolean debug) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = fileDepencencyDiscovery;
        this.debug = debug;
    }

    @Override
    public String getDescription() {
        return "Migrating " + buildFile;
    }

    @Override
    public void execute() throws Exception {
        List<String> args = new ArrayList<>();
        args.add("-bf");
        args.add(buildFile.getAbsolutePath());
        args.add("-pkg");
        args.add(pkg);
        args.add("-od");
        args.add(outDir.getAbsolutePath());
        args.add("-m");

        if (!fileDepencencyDiscovery)
            args.add("-noFD");
        if (debug)
            args.add("-d");

        AntMigrator.main(args.toArray(new String[] {}));
    }
}
