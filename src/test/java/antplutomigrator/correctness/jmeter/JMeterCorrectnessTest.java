package antplutomigrator.correctness.jmeter;

import antplutomigrator.correctness.comparison.*;
import antplutomigrator.correctness.utils.TaskExecutor;
import antplutomigrator.correctness.utils.tasks.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by manuel on 01.06.17.
 */
public class JMeterCorrectnessTest {
    private Log log = LogFactory.getLog(JMeterCorrectnessTest.class);

    private boolean debug = false;

    URL url = new URL("ftp://ftp.fau.de/apache//jmeter/source/apache-jmeter-3.2_src.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/jmeter.zip");
    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/jmeter/");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "apache-jmeter-3.2/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "apache-jmeter-3.2/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "apache-jmeter-3.2");
    File plutoSrcDir = new File(plutoDir, "apache-jmeter-3.2");

    public JMeterCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectness1() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "d5936f4f471b6b84c0d7f8b5c75ea72d", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.jmeter", false, debug, Arrays.asList("download_jars", "compile"));
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath + ":" + new File(JavaEnvUtils.getJavaHome()).getParent() + "/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/jmeter/JMeter.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[]{"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "lib"), new File(plutoSrcDir, "lib"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));

        taskExecutor.addTask(comparerTask);

        ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "build"), new File(plutoSrcDir, "build"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(buildComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new FileComparer() {
            @Override
            public boolean filesAreEqual(File f1, File f2) {
                // Ignore this file for now...
                if (f1.getName().endsWith("commons-daemon-1.0.15-native-src.tar.gz"))
                    return true;
                return false;
            }
        });
        taskExecutor.addTask(buildComparerTask);

        taskExecutor.executeTasks();
    }
}
