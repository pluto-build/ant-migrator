package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DockerRunnerTask extends TestTask {
    private final List<Mount> mounts;
    private final String command;
    private final String name;
    private final File localWorkingDir;
    private final File workingDir;

    public DockerRunnerTask(File localWorkingDir, String name, String command, File workingDir, List<Mount> mounts) {
        this.mounts = mounts;
        assert(this.mounts != null);
        this.command = command;
        this.name = name;
        this.localWorkingDir = localWorkingDir;
        this.workingDir = workingDir;
    }

    public DockerRunnerTask(File localWorkingDir, String name, String command, File workingDir) {
        this.command = command;
        this.name = name;
        this.mounts = new ArrayList<>();
        this.localWorkingDir = localWorkingDir;
        this.workingDir = workingDir;
    }

    @Override
    public String getDescription() {
        return "Running " + command + " in Docker (" + name + ")";
    }

    @Override
    public void execute() throws Exception {
        new RemoveDockerContainerTask(name).execute();
        String mountStr = "";
        for (Mount mount: mounts)
            mountStr += mount;
        new RunCommandTask(localWorkingDir,"docker run -i --privileged=true --security-opt=seccomp:unconfined "+mountStr+" -w "+workingDir.getAbsolutePath()+" --name "+name+" xmanu/pluto-docker:0.2 bash -c \""+command+"\"").execute();
        new RemoveDockerContainerTask(name).execute();
    }
}
