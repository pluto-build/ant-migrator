package antplutomigrator.correctness.commonsdaemon;

import antplutomigrator.correctness.comparison.FileComparer;
import antplutomigrator.correctness.comparison.MD5FileComparer;
import antplutomigrator.correctness.comparison.UnzipFileComparer;
import antplutomigrator.correctness.utils.TaskExecutor;
import antplutomigrator.correctness.utils.tasks.*;
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
public class CommonsDaemonCorrectnessTest {
    private Log log = LogFactory.getLog(CommonsDaemonCorrectnessTest.class);

    @Test
    public void testCorrectness1() throws Exception {
        URL commonsioZipUrl = new URL("http://mirror.dkd.de/apache//commons/daemon/source/commons-daemon-1.0.15-src.zip");

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
        taskExecutor.addTask(new FileDownloadTask(commonsioZipUrl, commonsioZipFile));
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
        taskExecutor.addTask(distComparerTask);

        taskExecutor.executeTasks();
    }
}
