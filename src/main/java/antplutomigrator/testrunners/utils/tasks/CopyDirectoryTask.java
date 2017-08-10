package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class CopyDirectoryTask extends TestTask {
    private final File src;
    private final File dst;

    public CopyDirectoryTask(File src, File dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public String getDescription() {
        return "Copying " + src + " to " + dst;
    }

    @Override
    public void execute() throws Exception {
        FileUtils.copyDirectoryToDirectory(src, dst);
    }
}
