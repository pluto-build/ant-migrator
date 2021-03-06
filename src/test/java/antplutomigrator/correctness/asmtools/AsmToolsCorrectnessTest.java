package antplutomigrator.correctness.asmtools;

import antplutomigrator.correctness.CorrectnessTest;
import antplutomigrator.testrunners.comparison.*;
import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.TestTask;
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
import java.util.Arrays;

/**
 * Created by manuel on 01.06.17.
 */
public class AsmToolsCorrectnessTest extends CorrectnessTest {
    File plutoBuildXml = new File(plutoSrcDir, "build/build.xml");
    File antBuildDir = new File(antSrcDir, "build");
    File plutoBuildDir = new File(plutoSrcDir, "build");


    public AsmToolsCorrectnessTest() throws Exception {
        super("asmtools.properties", false);
    }

    @Test
    public void testCorrectnessWithoutFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, md5, zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, pkg, false, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, migratedMainSrc, targetDir, classpath, pluto_compile_args));
        taskExecutor.addTask(new RunCommandTask(antBuildDir, ant_command));
        taskExecutor.addTask(new RunCommandTask(plutoBuildDir, pluto_run_command));

        ComparerTask comparerTask = new ComparerTask(new File(antDir, "asmtools-6.0-build"), new File(plutoDir, "asmtools-6.0-build"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
