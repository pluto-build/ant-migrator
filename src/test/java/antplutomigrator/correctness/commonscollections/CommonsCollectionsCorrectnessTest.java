package antplutomigrator.correctness.commonscollections;

import antplutomigrator.correctness.comparison.MD5FileComparer;
import antplutomigrator.correctness.comparison.UnzipFileComparer;
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

/**
 * Created by manuel on 01.06.17.
 */
public class CommonsCollectionsCorrectnessTest {
    private Log log = LogFactory.getLog(CommonsCollectionsCorrectnessTest.class);

    @Test
    public void testCorrectness1() throws Exception {
        URL commonsioZipUrl = new URL("http://mirror.serversupportforum.de/apache//commons/collections/source/commons-collections4-4.1-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonscollections/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-collections.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-collections4-4.1-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-collections4-4.1-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-collections4-4.1-src");
        File plutoSrcDir = new File(plutoDir, "commons-collections4-4.1-src");

        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new FileDownloadTask(commonsioZipUrl, commonsioZipFile));
        taskExecutor.addTask(new MD5CheckTask(commonsioZipFile, "6769b60edceefbfcae8e7519c32b24ca"));
        taskExecutor.addTask(new UnzipTask(commonsioZipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonscollections"));

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

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
