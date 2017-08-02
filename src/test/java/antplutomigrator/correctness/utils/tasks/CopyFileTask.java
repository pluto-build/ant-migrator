package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class CopyFileTask extends TestTask {

    private File src;
    private File dst;

    public CopyFileTask(File src, File dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public String getDescription() {
        return "Copying " + src.getAbsolutePath() + " to " + dst.getAbsolutePath();
    }

    @Override
    public void execute() throws Exception {
        FileUtils.copyFile(src,dst);
    }
}
