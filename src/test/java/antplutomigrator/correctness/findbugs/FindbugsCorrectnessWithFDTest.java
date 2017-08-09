package antplutomigrator.correctness.findbugs;

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
public class FindbugsCorrectnessWithFDTest {
    private Log log = LogFactory.getLog(FindbugsCorrectnessWithFDTest.class);

    private boolean debug = false;

    URL url = new URL("https://github.com/findbugsproject/findbugs/archive/3.0.1.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/downloads/findbugs.zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/findbugsfd/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File plutoDir = new File(testDir, "pluto");

    File antSrcDir = new File(antDir, "findbugs-3.0.1/findbugs");
    File plutoSrcDir = new File(plutoDir, "findbugs");
    File antBuildXml = new File(antSrcDir, "build.xml");
    File plutoBuildXml = new File(plutoSrcDir, "build.xml");
    File targetDir = new File(plutoDir, "target");

    public FindbugsCorrectnessWithFDTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "1b92b5626949833ce8d2f4dc490142a7", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("<property name=\"prop.plugin.jars\" refid=\"plugin.jars\"/>", "<property name=\"prop.plugin.jars\" value=\"\"/>");
        replacements.put("<equals arg1=\"1.8\" arg2=\"${ant.java.version}\"/>", "<equals arg1=\"1.7\" arg2=\"${ant.java.version}\"/>");
        replacements.put("<target name=\"-get-git-revision\">", "<target name=\"-get-git-revision\" if=\"DONTEXECUTE\">");
        taskExecutor.addTask(new ReplaceInLinesTask(antBuildXml, replacements));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.findbugs", true, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/findbugs/Findbugs.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "FindBugs_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "FindBugs_Pluto", plutoRunCommand, new File("/share/test/findbugs/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "build"), new File(plutoSrcDir, "build"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithoutFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "1b92b5626949833ce8d2f4dc490142a7", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("<property name=\"prop.plugin.jars\" refid=\"plugin.jars\"/>", "<property name=\"prop.plugin.jars\" value=\"\"/>");
        replacements.put("<equals arg1=\"1.8\" arg2=\"${ant.java.version}\"/>", "<equals arg1=\"1.7\" arg2=\"${ant.java.version}\"/>");
        replacements.put("<target name=\"-get-git-revision\">", "<target name=\"-get-git-revision\" if=\"DONTEXECUTE\">");
        taskExecutor.addTask(new ReplaceInLinesTask(antBuildXml, replacements));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.findbugs", false, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/findbugs/Findbugs.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "FindBugs_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "FindBugs_Pluto", plutoRunCommand, new File("/share/test/findbugs/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "build"), new File(plutoSrcDir, "build"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
