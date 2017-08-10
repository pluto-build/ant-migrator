package antplutomigrator.correctness.ddlutils;

import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.tasks.*;
import antplutomigrator.testrunners.comparison.*;
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
public class DdlUtilsCorrectnessTest {
    private Log log = LogFactory.getLog(DdlUtilsCorrectnessTest.class);

    private boolean debug = false;

    URL url = new URL("http://mirror.23media.de/apache/db/ddlutils/ddlutils-1.0/source/DdlUtils-1.0-src.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/ddlutils.zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/ddlutils/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "DdlUtils-1.0-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "DdlUtils-1.0-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "DdlUtils-1.0-src");
    File plutoSrcDir = new File(plutoDir, "DdlUtils-1.0-src");

    public DdlUtilsCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithoutFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "bd1aa777e70d4af3e99b36a09d413e1f", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, new File(antDir, "DdlUtils-1.0-src")));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.ddlutils", false, debug, Arrays.asList("jar"));
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/ddlutils/DdlUtils.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        ComparerTask comparerTaskDist = new ComparerTask(new File(antSrcDir, "dist"), new File(plutoSrcDir, "dist"));
        comparerTaskDist.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTaskDist.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTaskDist.getDirectoryComparer()));
        comparerTaskDist.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTaskDist);

        taskExecutor.executeTasks();
    }
}
