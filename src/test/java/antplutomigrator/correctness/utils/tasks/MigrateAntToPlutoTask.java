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
    private final List<String> targets;
    private boolean continueOnError = false;

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = false;
        this.debug = false;
        this.targets = null;
    }

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg, boolean fileDepencencyDiscovery, boolean debug) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = fileDepencencyDiscovery;
        this.debug = debug;
        this.targets = null;
    }

    public MigrateAntToPlutoTask(File buildFile, File outDir, String pkg, boolean fileDepencencyDiscovery, boolean debug, List<String> targets) {
        this.buildFile = buildFile;
        this.outDir = outDir;
        this.pkg = pkg;
        this.fileDepencencyDiscovery = fileDepencencyDiscovery;
        this.debug = debug;
        this.targets = targets;
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

        if (isContinueOnError())
            args.add("-c");

        if (!fileDepencencyDiscovery)
            args.add("-noFD");
        if (debug)
            args.add("-d");

        if (targets != null) {
            args.add("-t");

            String targetString = "";
            for (String target: targets)
                targetString += target + ",";
            if (targetString.endsWith(","))
                targetString = targetString.substring(0, targetString.length()-1);

            args.add(targetString);
        }

        AntMigrator.main(args.toArray(new String[] {}));
    }
}
