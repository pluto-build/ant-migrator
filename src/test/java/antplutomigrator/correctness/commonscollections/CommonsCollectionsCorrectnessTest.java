package antplutomigrator.correctness.commonscollections;

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
public class CommonsCollectionsCorrectnessTest {
    private Log log = LogFactory.getLog(CommonsCollectionsCorrectnessTest.class);

    private boolean debug = false;

    URL url = new URL("http://mirror.dkd.de/apache//commons/collections/source/commons-collections4-4.1-src.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/commons-collections.zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/commonscollections/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "commons-collections4-4.1-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "commons-collections4-4.1-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "commons-collections4-4.1-src");
    File plutoSrcDir = new File(plutoDir, "commons-collections4-4.1-src");

    public CommonsCollectionsCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectness1() throws Exception {

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6769b60edceefbfcae8e7519c32b24ca", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        final MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonscollections", false, debug, Arrays.asList("dist"));
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonscollections/Collections.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
