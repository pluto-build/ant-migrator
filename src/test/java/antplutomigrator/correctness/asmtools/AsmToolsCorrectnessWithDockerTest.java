package antplutomigrator.correctness.asmtools;

import antplutomigrator.correctness.CorrectnessTest;
import antplutomigrator.testrunners.comparison.*;
import antplutomigrator.testrunners.utils.TaskExecutor;
import antplutomigrator.testrunners.utils.tasks.*;
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

/**
 * Created by manuel on 01.06.17.
 */
public class AsmToolsCorrectnessWithDockerTest extends CorrectnessTest {
    File plutoBuildXml = new File(plutoSrcDir, "build/build.xml");


    public AsmToolsCorrectnessWithDockerTest() throws Exception {
        super("asmtools.properties", true);
    }

    @Test
    public void testCorrectnessWithFD() throws Exception {
        TaskExecutor taskExecutor = new TaskExecutor();

        taskExecutor.addTask(new DeleteDirTask(testDir));
        taskExecutor.addTask(new ProvideDownloadTask(url, md5, zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto."+projectName, true, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

       taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/"+projectName+"/"+projectMainName+".java"), targetDir, classpath, pluto_compile_args));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antDir, projectName+"_Ant", ant_command, new File("/share/test/"+projectSourceName+"/build/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, projectName+"_Pluto", pluto_run_command, new File("/share/test/"+projectSourceName+"/build/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antDir, "asmtools-6.0-build"), new File(plutoDir, "asmtools-6.0-build"));
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
        taskExecutor.addTask(new ProvideDownloadTask(url, md5, zipFile));
        taskExecutor.addTask(new UnzipTask(zipFile, antDir));
        taskExecutor.addTask(new CopyDirectoryTask(antSrcDir, plutoDir));
        MigrateAntToPlutoTask migrateAntToPlutoTask = new MigrateAntToPlutoTask(plutoBuildXml, plutoDir, "build.pluto."+projectName, false, debug);
        migrateAntToPlutoTask.setContinueOnError(true);
        migrateAntToPlutoTask.setCalculateStatistics(true);
        taskExecutor.addTask(migrateAntToPlutoTask);

        taskExecutor.addTask(new CompileJavaTask(plutoDir, new File(plutoDir, "build/pluto/"+projectName+"/"+projectMainName+".java"), targetDir, classpath, pluto_compile_args));

        List<Mount> mounts = new ArrayList<>();
        mounts.add(new Mount(antDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(antDir, projectName+"_Ant", ant_command, new File("/share/test/"+projectSourceName+"/build/"), mounts));


        mounts = new ArrayList<>();
        mounts.add(new Mount(plutoDir, new File("/share/test/")));
        mounts.add(new Mount(new File(System.getProperty("user.home")+"/.m2/"), new File("/share/m2/")));

        taskExecutor.addTask(new DockerRunnerTask(plutoDir, projectName+"_Pluto", pluto_run_command, new File("/share/test/"+projectSourceName+"/build/"), mounts));

        ComparerTask comparerTask = new ComparerTask(new File(antDir, "asmtools-6.0-build"), new File(plutoDir, "asmtools-6.0-build"));
        comparerTask.getDirectoryComparer().addFileComparer(new MD5FileComparer());
        comparerTask.getDirectoryComparer().addFileComparer(new UnzipFileComparer(comparerTask.getDirectoryComparer()));
        comparerTask.getDirectoryComparer().addFileComparer(new LineByLineFileComparer(Arrays.asList(new EqualLineComparer(), new JavaDocDateIgnoredLineComparer())));
        taskExecutor.addTask(comparerTask);

        taskExecutor.executeTasks();
    }
}
