package antplutomigrator.testrunners;

import antplutomigrator.testrunners.comparison.*;
import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.TestTask;
import antplutomigrator.testrunners.utils.tasks.*;
import org.apache.commons.io.FileUtils;
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

public class CommonsIORunner {
    private boolean debug = false;

    private final String classpath = "~/.m2/repository/org/apache/ant/ant/1.10.1/ant-1.10.1.jar:~/.m2/repository/build/pluto/pluto/1.10.0-SNAPSHOT/pluto-1.10.0-SNAPSHOT.jar:~/.m2/repository/org/sugarj/common/1.7.3-SNAPSHOT/common-1.7.3-SNAPSHOT.jar:~/.m2/repository/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar:~/.m2/repository/org/fusesource/jansi/jansi/1.14/jansi-1.14.jar:~/.m2/repository/org/objenesis/objenesis/2.2/objenesis-2.2.jar:~/.m2/repository/com/cedarsoftware/java-util-pluto-fixes/1.19.4-SNAPSHOT/java-util-pluto-fixes-1.19.4-20160107.071149-1.jar";
    private final String antCommand = "ant dist";
    private final String plutoCompileArgs = "-d <target> -sourcepath <source> <file> -cp <classpath>";
    private final String plutoRunCommand = "java -cp <classpath>:../target/. build.pluto.commonsio.CommonsIO";

    URL url = new URL("http://apache.lauf-forum.at/commons/io/source/commons-io-2.5-src.zip");

    File zipFile = new File("migrator-testdata/antplutomigrator/downloads/commons-io.zip");

    File testDir = new File("migrator-testdata/antplutomigrator/correctness/commonsiofd/");
    File sourceDir = new File(testDir,"source");
    File antDir = new File(testDir, "ant");
    File antBuildXml = new File(antDir, "commons-io-2.5-src/build.xml");
    File plutoDir = new File(testDir, "pluto");
    File plutoBuildXml = new File(plutoDir, "commons-io-2.5-src/build.xml");
    File targetDir = new File(plutoDir, "target");
    File antSrcDir = new File(antDir, "commons-io-2.5-src");
    File plutoSrcDir = new File(plutoDir, "commons-io-2.5-src");

    public CommonsIORunner() throws MalformedURLException {
    }

    public static void main(String[] args) {
        try {
            new CommonsIORunner().testCorrectnessWithoutFD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCorrectnessWithoutFD() throws Exception {
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

        String classPath = classpath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = CompileJavaTask.makeAbsolute(classPath);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/commonsio/CommonsIO.java"), targetDir, classPath, plutoCompileArgs));
        taskExecutor.addTask(new RunCommandTask(antSrcDir, antCommand));
        String finalPlutoRunCommand = CompileJavaTask.substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        taskExecutor.addTask(new RunCommandTask(plutoSrcDir, finalPlutoRunCommand));

        ComparerTask comparerTask = new ComparerTask(new File(antSrcDir, "target"), new File(plutoSrcDir, "target"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));

        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
