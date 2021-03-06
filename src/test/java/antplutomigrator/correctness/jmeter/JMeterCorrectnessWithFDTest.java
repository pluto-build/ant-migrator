package antplutomigrator.correctness.jmeter;

import antplutomigrator.testrunners.comparison.*;
import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.tasks.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Created by manuel on 01.06.17.
 */
public class JMeterCorrectnessWithFDTest {
    private Log log = LogFactory.getLog(JMeterCorrectnessWithFDTest.class);

    private boolean debug = false;

    URL url = new URL("ftp://ftp.fau.de/apache//jmeter/source/apache-jmeter-3.2_src.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/jmeter.zip");
    File src = new File("../migrator-testdata/antplutomigrator/apache-jmeter-3.2/");
    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/jmeterfd/");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "apache-jmeter-3.2/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "apache-jmeter-3.2/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "apache-jmeter-3.2");
    File plutoSrcDir = new File(plutoDir, "apache-jmeter-3.2");

    public JMeterCorrectnessWithFDTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        //fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "d5936f4f471b6b84c0d7f8b5c75ea72d", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        // Fix build.xml to not use non-relative paths
        taskExecutor.addTask(new MakePropertiesRelativeTask(antBuildXml));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));



        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.jmeter", true, debug, Arrays.asList("download_jars", "compile"));
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath + ":" + new File(JavaEnvUtils.getJavaHome()).getParent() + "/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/jmeter/JMeter.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antDir, "JMeter_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/apache-jmeter-3.2/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "JMeter_Pluto", plutoRunCommand, new File("/share/test/apache-jmeter-3.2/"), mounts));

        ComparerTask libComparerTask = new ComparerTask(new File(antSrcDir, "lib"), new File(plutoSrcDir, "lib"));
        libComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());

        taskExecutor.addTask(libComparerTask);

        ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "build"), new File(plutoSrcDir, "build"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(libComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(buildComparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithoutFD() throws Exception {
        //fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "d5936f4f471b6b84c0d7f8b5c75ea72d", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        // Fix build.xml to not use non-relative paths
        taskExecutor.addTask(new MakePropertiesRelativeTask(antBuildXml));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));



        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.jmeter", false, debug, Arrays.asList("download_jars", "compile"));
        migrateAntToPlutoTask.setContinueOnError(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath + ":" + new File(JavaEnvUtils.getJavaHome()).getParent() + "/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/jmeter/JMeter.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antDir, "JMeter_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/apache-jmeter-3.2/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "JMeter_Pluto", plutoRunCommand, new File("/share/test/apache-jmeter-3.2/"), mounts));

        ComparerTask libComparerTask = new ComparerTask(new File(antSrcDir, "lib"), new File(plutoSrcDir, "lib"));
        libComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());

        taskExecutor.addTask(libComparerTask);

        ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "build"), new File(plutoSrcDir, "build"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(libComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(buildComparerTask);

        taskExecutor.executeTasks();
    }
}
