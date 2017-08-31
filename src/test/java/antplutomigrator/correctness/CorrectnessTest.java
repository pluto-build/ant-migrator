package antplutomigrator.correctness;

import antplutomigrator.testrunners.utils.tasks.CompileJavaTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.net.URL;
import java.util.Properties;

public class CorrectnessTest {
    public Log log = LogFactory.getLog(this.getClass());
    public final boolean debug;
    public final boolean docker;
    public final Properties prop;

    public final String projectName;
    public final String projectSourceName;
    public final String projectMainName;
    public final URL url;
    public final String md5;
    public final String pkg;

    public final File zipFile;
    public File testDir;
    public File antDir;
    public File plutoDir;
    public final File antSrcDir;
    public final File plutoSrcDir;
    public final File antBuildXml;
    public final File plutoBuildXml;
    public final File migratedMainSrc;

    public final String classpath;
    public final String classpathDocker;
    public final String pluto_compile_args;
    public final String ant_command;
    public final String pluto_run_command;

    public final File targetDir;

    public CorrectnessTest(String configFile, boolean docker) throws Exception {
        this.docker = docker;

        this.prop = PropertyLoader.getProperties(configFile);

        debug = prop.getProperty("debug", "false").equals("true");

        projectName = prop.getProperty("projectName");
        projectSourceName = prop.getProperty("projectSourceName");
        projectMainName = prop.getProperty("projectMainName", projectName);
        url = new URL(prop.getProperty("url"));
        md5 = prop.getProperty("md5");
        pkg = "build.pluto."+projectName;

        testDir = new File("../migrator-testdata/antplutomigrator/correctness/" + ((docker) ? "docker" : "native") + "/" + projectName + "/");
        zipFile = new File("../migrator-testdata/antplutomigrator/downloads/" + url.getFile());
        antDir = new File(testDir, "ant");
        plutoDir = new File(testDir, "pluto");
        antSrcDir = new File(antDir, projectSourceName);
        plutoSrcDir = new File(plutoDir, projectSourceName);
        antBuildXml = new File(antSrcDir, "build.xml");
        plutoBuildXml = new File(plutoSrcDir, "build.xml");
        targetDir = new File(plutoDir, "target");
        migratedMainSrc = new File(plutoDir, "build/pluto/"+projectName+"/"+projectMainName+".java");

        classpath = CompileJavaTask.makeAbsolute(prop.getProperty("classpath")+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar");
        classpathDocker = prop.getProperty("classpath_docker");
        pluto_compile_args = prop.getProperty("pluto_compile_args");
        ant_command = prop.getProperty("ant_command");
        pluto_run_command = CompileJavaTask.substituteVars(prop.getProperty("pluto_run_command"), new String[] {"<classpath>", "<projectname>", "<projectmain>"}, new String[]{(docker)?classpathDocker:classpath, projectName, projectMainName});
    }
}
