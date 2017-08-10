package antplutomigrator.correctness.commonsio;

import antplutomigrator.correctness.comparison.*;
import antplutomigrator.correctness.utils.TaskExecutor;
import antplutomigrator.correctness.utils.TestTask;
import antplutomigrator.correctness.utils.tasks.*;
import org.apache.commons.io.FileUtils;
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
public class CommonsIOCorrectnessWithFDTest {
    private Log log = LogFactory.getLog(CommonsIOCorrectnessWithFDTest.class);

    private boolean debug = false;

    URL url = new URL("http://apache.lauf-forum.at/commons/io/source/commons-io-2.5-src.zip");

    File zipFile = new File("../migrator-testdata/antplutomigrator/commons-io.zip");

    File testDir = new File("../migrator-testdata/antplutomigrator/correctness/commonsiofd/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "commons-io-2.5-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "commons-io-2.5-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "commons-io-2.5-src");
    File plutoSrcDir = new File(plutoDir, "commons-io-2.5-src");

    public CommonsIOCorrectnessWithFDTest() throws MalformedURLException {
    }


    @Test
    public void testCorrectnessWithFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "bd1731c2655a9c46f4a01dd6b1ef24fb", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new TestTask() {
            @Override
            public String getDescription() {
                return "Patching build.xml";
            }

            @Override
            public void execute() throws Exception {
                String buildFileStr = FileUtils.readFileToString(antBuildXml);
                buildFileStr = buildFileStr.replaceAll("https://hamcrest\\.googlecode\\.com/files/hamcrest-core-1\\.3\\.jar", "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/hamcrest/hamcrest-core-1.3.jar");
                FileUtils.writeStringToFile(antBuildXml, buildFileStr);
                //TODO: WHY is this needed?!?!
                //Thread.sleep(200);
            }
        });
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        final MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsio", true, debug, Arrays.asList("dist"));
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsio/CommonsIO.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsIO_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsIO_Pluto", plutoRunCommand, new File("/share/test/commons-io-2.5-src/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessNoFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "bd1731c2655a9c46f4a01dd6b1ef24fb", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new TestTask() {
            @Override
            public String getDescription() {
                return "Patching build.xml";
            }

            @Override
            public void execute() throws Exception {
                String buildFileStr = FileUtils.readFileToString(antBuildXml);
                buildFileStr = buildFileStr.replaceAll("https://hamcrest\\.googlecode\\.com/files/hamcrest-core-1\\.3\\.jar", "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/hamcrest/hamcrest-core-1.3.jar");
                FileUtils.writeStringToFile(antBuildXml, buildFileStr);
                //TODO: WHY is this needed?!?!
                //Thread.sleep(200);
            }
        });
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsio", false, debug, Arrays.asList("dist")));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsio/CommonsIO.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsIO_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsIO_Pluto", plutoRunCommand, new File("/share/test/commons-io-2.5-src/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }

    @Test
    public void testCorrectnessWithFDReRun() throws Exception {
        fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "bd1731c2655a9c46f4a01dd6b1ef24fb", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new TestTask() {
            @Override
            public String getDescription() {
                return "Patching build.xml";
            }

            @Override
            public void execute() throws Exception {
                String buildFileStr = FileUtils.readFileToString(antBuildXml);
                buildFileStr = buildFileStr.replaceAll("https://hamcrest\\.googlecode\\.com/files/hamcrest-core-1\\.3\\.jar", "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/hamcrest/hamcrest-core-1.3.jar");
                FileUtils.writeStringToFile(antBuildXml, buildFileStr);
                //TODO: WHY is this needed?!?!
                //Thread.sleep(200);
            }
        });
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        taskExecutor.addTask(new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto.commonsio", true, debug, Arrays.asList("dist")));

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsio/CommonsIO.java"), targetDir, classPath, new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())))));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        //taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsIO_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        String classPathDocker = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath_fd.txt").toURI())));

        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{classPathDocker});
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsIO_Pluto", plutoRunCommand, new File("/share/test/commons-io-2.5-src/"), mounts));

//        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
//        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
//        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
//        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new AntVersionIgnoredLineComparer())));
//
//        taskExecutor.addTask(comparerTask);

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsIO_Pluto", plutoRunCommand, new File("/share/test/commons-io-2.5-src/"), mounts));
        taskExecutor.addTask(new DeleteFileTask(new File(plutoSrcDir, "/target/commons-io-2.5-SNAPSHOT.jar")));
        taskExecutor.addTask(new DockerRunnerTask(plutoDir, "CommonsIO_Pluto", plutoRunCommand, new File("/share/test/commons-io-2.5-src/"), mounts));

        taskExecutor.executeTasks();
    }

    @Test
    public void testAntIncremental() throws Exception {
        fail();
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, "bd1731c2655a9c46f4a01dd6b1ef24fb", zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antSrcDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsIO_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        taskExecutor.addTask(new DeleteFileTask(new File(antSrcDir, "/target/commons-io-2.5-SNAPSHOT.jar")));

        taskExecutor.addTask(new DockerRunnerTask(antSrcDir, "CommonsIO_Ant", new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI()))), new File("/share/test/"), mounts));

        taskExecutor.executeTasks();
    }
}
