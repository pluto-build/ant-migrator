package antplutomigrator.testrunners.utils.tasks;

import java.io.File;

public class Mount {
    private File local;
    private File docker;

    public File getLocal() {
        return local;
    }

    public File getDocker() {
        return docker;
    }

    public Mount(File local, File docker) {
        this.local = local;
        this.docker = docker;
    }

    @Override
    public String toString() {
        return "-v " + local.getAbsolutePath()+":"+docker.getAbsolutePath() + " ";
    }
}