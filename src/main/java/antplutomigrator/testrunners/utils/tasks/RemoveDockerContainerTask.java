package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.io.IOUtils;

import java.io.File;

import static org.junit.Assert.assertEquals;

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

        Process antProcess = new ProcessBuilder().directory(new File(".")).command("docker", "container", "rm", name).start();
        int res = antProcess.waitFor();

        if (res == 1) {
            assert IOUtils.toString(antProcess.getErrorStream()).contains("No such container");
        } else
            assert res == 0;

        //new RunCommandTask(new File("."), "docker container rm " + name).execute();
    }
}
