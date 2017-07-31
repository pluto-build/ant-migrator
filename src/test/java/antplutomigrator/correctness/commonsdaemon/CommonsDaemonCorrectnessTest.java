package antplutomigrator.correctness.commonsdaemon;

import antplutomigrator.correctness.comparison.*;
import antplutomigrator.correctness.utils.TaskExecutor;
import antplutomigrator.correctness.utils.TestTask;
import antplutomigrator.correctness.utils.tasks.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by manuel on 01.06.17.
 */
public class CommonsDaemonCorrectnessTest {
    private Log log = LogFactory.getLog(CommonsDaemonCorrectnessTest.class);

    private boolean debug = false;

    @Test
    public void testCorrectness1() throws Exception {
        URL commonsdaemonZipUrl = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonsdaemon/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-daemon.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-daemon-1.0.15-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-daemon-1.0.15-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-daemon-1.0.15-src");
        File plutoSrcDir = new File(plutoDir, "commons-daemon-1.0.15-src");

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new FileDownloadTask(commonsdaemonZipUrl, commonsioZipFile));
        taskExecutor.addTask(new MD5CheckTask(commonsioZipFile, "9c6580f437a429d3694a5a3214cc83c1"));
        taskExecutor.addTask(new UnzipTask(commonsioZipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsdaemon"));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsdaemon/Daemon.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));

        taskExecutor.addTask(comparerTask);

        ComparerTask distComparerTask = new ComparerTask(new File(antSrcDir, "dist"), new File(plutoSrcDir, "dist"));
        distComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        distComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        distComparerTask.getDirectoryComparer().addFileComparer(new FileComparer() {
            @Override
            public boolean filesAreEqual(File f1, File f2) {
                // Ignore this file for now...
                if (f1.getName().endsWith("commons-daemon-1.0.15-native-src.tar.gz"))
                    return true;
                return false;
            }
        });
        distComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(distComparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessnoFDRerun() throws Exception {
        URL commonsdaemonZipUrl = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonsdaemon/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-daemon.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-daemon-1.0.15-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-daemon-1.0.15-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-daemon-1.0.15-src");
        File plutoSrcDir = new File(plutoDir, "commons-daemon-1.0.15-src");

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new FileDownloadTask(commonsdaemonZipUrl, commonsioZipFile));
        taskExecutor.addTask(new MD5CheckTask(commonsioZipFile, "9c6580f437a429d3694a5a3214cc83c1"));
        taskExecutor.addTask(new UnzipTask(commonsioZipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsdaemon", false, true));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsdaemon/Daemon.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        URL commonsdaemonZipUrl = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonsdaemonfd/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-daemon.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-daemon-1.0.15-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-daemon-1.0.15-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-daemon-1.0.15-src");
        File plutoSrcDir = new File(plutoDir, "commons-daemon-1.0.15-src");

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new FileDownloadTask(commonsdaemonZipUrl, commonsioZipFile));
        taskExecutor.addTask(new MD5CheckTask(commonsioZipFile, "9c6580f437a429d3694a5a3214cc83c1"));
        taskExecutor.addTask(new UnzipTask(commonsioZipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsdaemon", true, debug));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsdaemon/Daemon.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsDaemon_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsDaemon_Pluto", plutoRunCommand, new File("/share/test/commons-daemon-1.0.15-src/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithFDRerun() throws Exception {
        URL commonsdaemonZipUrl = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonsdaemonfd/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-daemon.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-daemon-1.0.15-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-daemon-1.0.15-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-daemon-1.0.15-src");
        File plutoSrcDir = new File(plutoDir, "commons-daemon-1.0.15-src");

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new FileDownloadTask(commonsdaemonZipUrl, commonsioZipFile));
        taskExecutor.addTask(new MD5CheckTask(commonsioZipFile, "9c6580f437a429d3694a5a3214cc83c1"));
        taskExecutor.addTask(new UnzipTask(commonsioZipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsdaemon", true, debug));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsdaemon/Daemon.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsDaemon_Pluto", plutoRunCommand, new File("/share/test/commons-daemon-1.0.15-src/"), mounts));

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsDaemon_Pluto", plutoRunCommand, new File("/share/test/commons-daemon-1.0.15-src/"), mounts));

        taskExecutor.addTask(new TestTask() {
            @Override
            public String getDescription() {
                return "delete commons-daemon-1.0.15.jar";
            }

            @Override
            public void execute() throws Exception {
                assert new File(plutoSrcDir, "/dist/commons-daemon-1.0.15.jar").delete() == true;
            }
        });

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsDaemon_Pluto", plutoRunCommand, new File("/share/test/commons-daemon-1.0.15-src/"), mounts));

        taskExecutor.executeTasks();
    }
}
