package antplutomigrator.correctness.tomcat;

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
import java.util.HashMap;

/**
 * Created by manuel on 01.06.17.
 */
public class TomcatCorrectnessTest {
    private Log log = LogFactory.getLog(TomcatCorrectnessTest.class);

    private boolean debug = false;

    URL url = new URL("https://github.com/Debian/tomcat8/archive/master.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/tomcat.zip");
    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/tomcat/");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "tomcat8-master/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "tomcat8-master/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "tomcat8-master");
    File plutoSrcDir = new File(plutoDir, "tomcat8-master");

    public TomcatCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectness1() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "8909798e8c52d54c385f48ace62a3fb0", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        HashMap<String, String> replacements = new HashMap();
        replacements.put("<antcall target=\"examples-sources\" />", "");
        taskExecutor.addTask(new ReplaceInLinesTask(antBuildXml, replacements));

        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.tomcat", false, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath + ":" + new File(JavaEnvUtils.getJavaHome()).getParent() + "/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/tomcat/Tomcat8_5.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())))));
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[]{"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, plutoRunCommand));

        ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "output"), new File(plutoSrcDir, "output"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(buildComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new ServerInfoDateIgnoredLineComparer())));

        taskExecutor.addTask(buildComparerTask);

        taskExecutor.executeTasks();
    }
}
