package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class UnzipTask extends TestTask {

    private final File file;
    private final File destDir;

    public UnzipTask(File file, File destDir) {
        this.file = file;
        this.destDir = destDir;
    }

    @Override
    public String getDescription() {
        return "Unzipping " + file + " to " + destDir;
    }

    @Override
    public void execute() throws Exception {
        ZipFile zipFile = new ZipFile(file);
        zipFile.setRunInThread(true);
        zipFile.extractAll(destDir.getAbsolutePath());
        while (zipFile.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
            Thread.sleep(10);
    }
}
