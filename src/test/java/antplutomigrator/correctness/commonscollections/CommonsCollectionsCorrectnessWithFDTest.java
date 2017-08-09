package antplutomigrator.correctness.commonscollections;

import antplutomigrator.correctness.comparison.*;
import antplutomigrator.correctness.utils.TaskExecutor;
import antplutomigrator.correctness.utils.TestTask;
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
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Created by manuel on 01.06.17.
 */
public class CommonsCollectionsCorrectnessWithFDTest {
    private Log log = LogFactory.getLog(CommonsCollectionsCorrectnessWithFDTest.class);

    private boolean debug = true;

    URL url = new URL("http://mirror.dkd.de/apache//commons/collections/source/commons-collections4-4.1-src.zip");
    File zipsrc = new File("../migrator-testdata/antplutomigrator/downloads/commons-collections.zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/commonscollectionsfd/");
    File sourceDir = new File(testDir,"source");
    File zipFile = new File(testDir, "commons-collections.zip");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "commons-collections4-4.1-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "commons-collections4-4.1-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "commons-collections4-4.1-src");
    File plutoSrcDir = new File(plutoDir, "commons-collections4-4.1-src");

    public CommonsCollectionsCorrectnessWithFDTest() throws MalformedURLException {
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6769b60edceefbfcae8e7519c32b24ca", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));

        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonscollections", true, debug);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonscollections/Collections.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        //taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsCollections_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsCollections_Pluto", plutoRunCommand, new File("/share/test/commons-collections4-4.1-src/"), mounts));

        /*ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);*/

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessNoFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6769b60edceefbfcae8e7519c32b24ca", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonscollections", false, debug));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonscollections/Collections.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsCollections_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsCollections_Pluto", plutoRunCommand, new File("/share/test/commons-collections4-4.1-src/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithFDRerun() throws Exception {
        fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6769b60edceefbfcae8e7519c32b24ca", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonscollections", true, debug));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonscollections/Collections.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsCollections_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsCollections_Pluto", plutoRunCommand, new File("/share/test/commons-collections4-4.1-src/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsCollections_Pluto", plutoRunCommand, new File("/share/test/commons-collections4-4.1-src/"), mounts));

        taskExecutor.addTask(new DeleteFileTask(new File(antSrcDir, "/target/classes/org/apache/commons/collections4/Bag.class")));

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsCollections_Pluto", plutoRunCommand, new File("/share/test/commons-collections4-4.1-src/"), mounts));

        taskExecutor.executeTasks();
    }

    @Test
    public void testAntIncremental() throws Exception {
        fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "6769b60edceefbfcae8e7519c32b24ca", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsCollections_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        taskExecutor.addTask(new DeleteFileTask(new File(antSrcDir, "/target/classes/org/apache/commons/collections4/Bag.class")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsCollections_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        taskExecutor.executeTasks();
    }
}
