package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class DeleteDirTask extends TestTask {
    private final File dir;

    public DeleteDirTask(File dir) {
        assert !dir.exists() || dir.isDirectory();
        this.dir = dir;
    }

    @Override
    public String getDescription() {
        return "Deleting " + dir;
    }

    @Override
    public void execute() throws Exception {
        FileUtils.deleteDirectory(dir);
    }
}
