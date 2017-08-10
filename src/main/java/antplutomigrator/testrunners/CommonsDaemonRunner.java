package antplutomigrator.testrunners;

import antplutomigrator.testrunners.comparison.*;
import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.TestTask;
import antplutomigrator.testrunners.utils.tasks.*;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class CommonsDaemonRunner {
    private boolean debug = false;

    private final String antCommand = "ant";
    private final String plutoCompileArgs = "-d <target> -sourcepath <source> <file> -cp <classpath>";
    private final String plutoRunCommand = "java -cp <classpath>:../target/. build.pluto.commonsdaemon.Daemon";

    URL url = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

    File zipFile = new File("migrator-testdata/antplutomigrator/downloads/commons-daemon.zip");
    File testDir = new File("migrator-testdata/antplutomigrator/correctness/commonsdaemon/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "commons-daemon-1.0.15-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "commons-daemon-1.0.15-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "commons-daemon-1.0.15-src");
    File plutoSrcDir = new File(plutoDir, "commons-daemon-1.0.15-src");

    public boolean fdDiscovery = true;

    public CommonsDaemonRunner() throws MalformedURLException {
    }

    public static void main(String[] args) {
        try {
            CommonsDaemonRunner runner = new CommonsDaemonRunner();
            if (args.length > 1) {
                runner.fdDiscovery = !args[1].equals("-noFD");
            }
            runner.testCorrectnessWithoutFD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCorrectnessWithoutFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "9c6580f437a429d3694a5a3214cc83c1", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        final MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsdaemon", true, debug);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String classPath = "";
        for (File file: new File(".").listFiles()) {
            if (file.getName().endsWith(".jar")) {
                classPath += file.getAbsolutePath() + ":";
            }
        }
        classPath+=new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        System.out.println(classPath);
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsdaemon/Daemon.java"), targetDir, classPath, plutoCompileArgs));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, antCommand));
        String finalPlutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, finalPlutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
