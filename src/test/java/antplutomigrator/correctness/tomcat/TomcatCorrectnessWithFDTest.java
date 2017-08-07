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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by manuel on 01.06.17.
 */
public class TomcatCorrectnessWithFDTest {
    private Log log = LogFactory.getLog(TomcatCorrectnessWithFDTest.class);

    private boolean debug = false;

    URL url = new URL("https://github.com/Debian/tomcat8/archive/debian/8.5.16-1.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/tomcat.zip");
    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/tomcatfd/");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "tomcat8-debian-8.5.16-1/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "tomcat8-debian-8.5.16-1/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "tomcat8-debian-8.5.16-1");
    File plutoSrcDir = new File(plutoDir, "tomcat8-debian-8.5.16-1");

    public TomcatCorrectnessWithFDTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6dfb297a2f6e729cd773e036167212cd", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));

        HashMap<String, String> replacements = new HashMap();
        replacements.put("<antcall target=\"examples-sources\" />", "");
        taskExecutor.addTask(new ReplaceInLinesTask(antBuildXml, replacements));

        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.tomcat", true, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath + ":" + new File(JavaEnvUtils.getJavaHome()).getParent() + "/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/tomcat/Tomcat8_5.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antDir, "Tomcat_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/tomcat8-debian-8.5.16-1/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "Tomcat_Pluto", plutoRunCommand, new File("/share/test/tomcat8-debian-8.5.16-1/"), mounts));

        ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "output"), new File(plutoSrcDir, "output"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(buildComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new ServerInfoDateIgnoredLineComparer())));

        taskExecutor.addTask(buildComparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessNoFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6dfb297a2f6e729cd773e036167212cd", zipFile));
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

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        //taskExecutor.addTask(new DockerRunnerTask(antDir, "Tomcat_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/tomcat8-debian-8.5.16-1/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "Tomcat_Pluto", plutoRunCommand, new File("/share/test/tomcat8-debian-8.5.16-1/"), mounts));

        /*ComparerTask buildComparerTask = new ComparerTask(new File(antSrcDir, "output"), new File(plutoSrcDir, "output"));
        buildComparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        buildComparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(buildComparerTask.getDirectoryComparer()));
        buildComparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new ServerInfoDateIgnoredLineComparer())));

        taskExecutor.addTask(buildComparerTask);*/

        taskExecutor.executeTasks();
    }
}
