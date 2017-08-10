package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;

import java.io.File;

public class DeleteFileTask extends TestTask {

    private final File file;

    public DeleteFileTask(File file) {
        this.file = file;
    }

    @Override
    public String getDescription() {
        return "Deleting " + file.getAbsolutePath();
    }

    @Override
    public void execute() throws Exception {
        assert file.delete() == true;
    }
}
