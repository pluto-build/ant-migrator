package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Created by manuel on 08.06.17.
 */
public class RunCommandTask extends TestTask {
    private final String command;
    private final File workingDir;

    public RunCommandTask(File workingDir, String command) {
        this.workingDir = workingDir;
        this.command = command;
    }

    @Override
    public String getDescription() {
        return "Running " + command;
    }

    @Override
    public void execute() throws Exception {
        Process antProcess = new ProcessBuilder().directory(workingDir).command(command.split(" ")).inheritIO().start();
        assertEquals(0, antProcess.waitFor());
    }
}
