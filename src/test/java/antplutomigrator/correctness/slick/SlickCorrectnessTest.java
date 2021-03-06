package antplutomigrator.correctness.slick;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by manuel on 01.06.17.
 */
public class SlickCorrectnessTest {
    private Log log = LogFactory.getLog(SlickCorrectnessTest.class);

    private boolean debug = false;

    private String projectName = "slick";
    private String projectSourceName = "slick";
    private String projectMainName = "Slick";
    private String md5Hash = "203a24252a1e18d1b52b1d7f27d10a34";

    URL url = new URL("http://slick.ninjacave.com/slick.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/"+projectName+".zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/"+projectName+"/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, projectSourceName +"/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, projectSourceName +"/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, projectSourceName);
    File plutoSrcDir = new File(plutoDir, projectSourceName);

    public SlickCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithoutFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, md5Hash, zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, new File(antDir, "slick")));
        taskExecutor.addTask(new TestTask() {
            @Override
            public String getDescription() {
                return "Create examples dir";
            }

            @Override
            public void execute() throws Exception {
                Files.createDirectories(new File(antSrcDir, "examples").toPath());
            }
        });
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto."+projectName, false, debug, Arrays.asList("dist"));
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/"+projectName+"/"+projectMainName+".java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>", "<projectname>", "<projectmain>"}, new String[]{absoluteClassPath, projectName, projectMainName});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(comparerTask);

        ComparerTask distComparerTask = new ComparerTask(new File(antSrcDir, "dist"), new File(plutoSrcDir, "dist"));
        distComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        distComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(distComparerTask.getDirectoryComparer()));
        distComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(distComparerTask);

        taskExecutor.executeTasks();
    }
}
