package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;

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
