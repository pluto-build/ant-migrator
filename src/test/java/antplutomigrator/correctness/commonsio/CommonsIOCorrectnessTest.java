package antplutomigrator.correctness.commonsio;

import antplutomigrator.correctness.comparison.ComparisonException;
import antplutomigrator.correctness.comparison.DirectoryComparer;
import antplutomigrator.runner.AntMigrator;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by manuel on 01.06.17.
 */
public class CommonsIOCorrectnessTest {
    private Log log = LogFactory.getLog(CommonsIOCorrectnessTest.class);

    @Test
    public void testCorrectness1() throws Exception {
        URL commonsioZipUrl = new URL("http://apache.lauf-forum.at/commons/io/source/commons-io-2.5-src.zip");

        File testDir = new File("testdata/antplutomigrator/correctness/commonsio/");
        File sourceDir = new File(testDir,"source");
        File commonsioZipFile = new File(testDir, "commons-io.zip");
        File antDir = new File(testDir, "ant");
        File antBuildXml = new File(antDir, "commons-io-2.5-src/build.xml");
        File plutoDir = new File(testDir, "pluto");
        File plutoBuildXml = new File(plutoDir, "commons-io-2.5-src/build.xml");
        File targetDir = new File(plutoDir, "target");
        File antSrcDir = new File(antDir, "commons-io-2.5-src");
        File plutoSrcDir = new File(plutoDir, "commons-io-2.5-src");

        System.out.println("Cleaning...");
        FileUtils.deleteDirectory(testDir);

        System.out.println("Downloading commons-io...");
        FileUtils.copyURLToFile(commonsioZipUrl, commonsioZipFile);

        System.out.println("Unzipping sources...");
        ZipFile zipFile = new ZipFile(commonsioZipFile);
        zipFile.setRunInThread(true);
        zipFile.extractAll(antDir.getAbsolutePath());
        while (zipFile.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
            Thread.sleep(10);

        System.out.println("Patching build.xml...");
        String buildFileStr = FileUtils.readFileToString(antBuildXml);
        buildFileStr = buildFileStr.replaceAll("https://hamcrest\\.googlecode\\.com/files/hamcrest-core-1\\.3\\.jar", "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/hamcrest/hamcrest-core-1.3.jar");
        FileUtils.writeStringToFile(antBuildXml, buildFileStr);
        //TODO: WHY is this needed?!?!
        Thread.sleep(200);

        System.out.println("Copy to pluto directory...");
        FileUtils.copyDirectoryToDirectory(antSrcDir, plutoDir);

        System.out.println("Migrating build.xml...");
        AntMigrator.main(new String[] {"-bf", plutoBuildXml.getAbsolutePath(), "-noFD", "-pkg", "build.pluto.commonsio", "-od", plutoDir.getAbsolutePath(), "-m"});

        System.out.println("Compiling pluto build...");
        targetDir.mkdirs();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        String readClassPath = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("classpath.txt").toURI())));
        String classPath = readClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";
        String absoluteClassPath = makeAbsolute(classPath);

        String readCompileArgs = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_compile_args.txt").toURI())));
        readCompileArgs = substituteVars(readCompileArgs, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        String[] compilerArgs = readCompileArgs.split(" ");
        compiler.run(null, null, null, compilerArgs);

        System.out.println("Running ant...");
        long start = System.currentTimeMillis();
        String readAntCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("ant_command.txt").toURI())));
        Process antProcess = new ProcessBuilder().directory(antSrcDir).command(readAntCommand).inheritIO().start();
        assertEquals(0, antProcess.waitFor());
        System.out.println("Took " + (System.currentTimeMillis()-start) + "ms.");

        System.out.println("Running pluto...");
        String plutoRunCommand = new String(Files.readAllBytes(Paths.get(this.getClass().getResource("pluto_run_command.txt").toURI())));
        plutoRunCommand = substituteVars(plutoRunCommand, new String[] {"<classpath>"}, new String[]{absoluteClassPath});
        Process plutoProcess = new ProcessBuilder().directory(plutoSrcDir).command(plutoRunCommand.split(" ")).inheritIO().start();
        assertEquals(0, plutoProcess.waitFor());

        System.out.println("Comparing...");
        DirectoryComparer directoryComparer = new DirectoryComparer(Arrays.asList(new File(antSrcDir, "target"), new File(plutoSrcDir, "target")));
        directoryComparer.compare(((f1, f2) -> {
            if (f1.getName().endsWith("jar")) {
                System.err.println("Found differently hashed jars. Doing deep diff. ("+ f1 + " ≠ " + f2 + ")");
                File tempDir = FileUtils.getTempDirectory();
                File f1Tmp = new File(tempDir, "f1");
                File f2Tmp = new File(tempDir, "f2");
                ZipFile f1z = new ZipFile(f1);
                f1z.setRunInThread(true);
                f1z.extractAll(f1Tmp.getAbsolutePath());
                while (f1z.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
                    Thread.sleep(10);
                ZipFile f2z = new ZipFile(f2);
                f2z.setRunInThread(true);
                f2z.extractAll(f2Tmp.getAbsolutePath());
                while (f2z.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
                    Thread.sleep(10);

                DirectoryComparer directoryComparer1 = new DirectoryComparer(Arrays.asList(f1Tmp, f2Tmp));
                try {
                    // We don't supported nested jar differences here...
                    directoryComparer1.compare((f11, f21) -> false);
                } catch (ComparisonException e) {
                    return false;
                } finally {
                    FileUtils.deleteDirectory(f1Tmp);
                    FileUtils.deleteDirectory(f2Tmp);
                }
                return true;
            }
            return false;
        }
        ));
    }

    private static String substituteVars(String str, String[] vars, String[] repl) {
        String ret = str;
        for (int i = 0; i < vars.length; i++){
            ret = ret.replaceAll(vars[i], repl[i]);
        }
        return ret;
    }

    private static String makeAbsolute(String classPath) {
        String[] classPathElements = classPath.split(":");
        StringBuilder absoluteClassPath = new StringBuilder();
        for (String cpe: classPathElements) {
            File cpeFile = new File(cpe.replaceFirst("^~",System.getProperty("user.home")));
            absoluteClassPath.append(cpeFile.getAbsolutePath()).append(":");
        }
        if (absoluteClassPath.toString().endsWith(":"))
            return absoluteClassPath.substring(0, absoluteClassPath.length()-1);
        return absoluteClassPath.toString();
    }
}
