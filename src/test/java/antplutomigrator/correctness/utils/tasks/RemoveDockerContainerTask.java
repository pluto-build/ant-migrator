package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;

import java.io.File;

public class RemoveDockerContainerTask extends TestTask {
    private final String name;

    public RemoveDockerContainerTask(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return "Removing Docker container " + name;
    }

    @Override
    public void execute() throws Exception {
        new RunCommandTask(new File("."), "docker container rm " + name).execute();
    }
}
