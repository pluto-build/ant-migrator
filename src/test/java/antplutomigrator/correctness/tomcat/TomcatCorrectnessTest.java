package antplutomigrator.correctness.tomcat;

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
import java.util.HashMap;

/**
 * Created by manuel on 01.06.17.
 */
public class TomcatCorrectnessTest {
    private Log log = LogFactory.getLog(TomcatCorrectnessTest.class);

    private boolean debug = false;

    URL url = new URL("https://github.com/Debian/tomcat8/archive/debian/8.5.16-1.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/tomcat.zip");
    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/tomcat/");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "tomcat8-debian-8.5.16-1/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "tomcat8-debian-8.5.16-1/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "tomcat8-debian-8.5.16-1");
    File plutoSrcDir = new File(plutoDir, "tomcat8-debian-8.5.16-1");
    File antLibsDir = new File(antDir, "libs");
    File plutoLibsDir = new File(plutoDir, "libs");

    public TomcatCorrectnessTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectness1() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6dfb297a2f6e729cd773e036167212cd", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("<antcall target=\"examples-sources\" />", "");
        taskExecutor.addTask(new ReplaceInLinesTask(antBuildXml, replacements));

        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));

        HashMap<String, String> replacementProperties = new HashMap<>();
        replacementProperties.put("base.path=${user.home}/tomcat-build-libs", "base.path="+antLibsDir.getAbsolutePath());
        taskExecutor.addTask(new ReplaceInLinesTask(new File(antSrcDir, "build.properties.default"), replacementProperties));

        HashMap<String, String> replacementPropertiesPluto = new HashMap<>();
        replacementPropertiesPluto.put("base.path=${user.home}/tomcat-build-libs", "base.path="+plutoLibsDir.getAbsolutePath());
        taskExecutor.addTask(new ReplaceInLinesTask(new File(plutoSrcDir, "build.properties.default"), replacementPropertiesPluto));

        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.tomcat", false, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
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

        ComparerTask libComparerTask = new ComparerTask(antLibsDir, plutoLibsDir);
        libComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        libComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(libComparerTask.getDirectoryComparer()));

        taskExecutor.addTask(libComparerTask);

        taskExecutor.executeTasks();
    }
}
