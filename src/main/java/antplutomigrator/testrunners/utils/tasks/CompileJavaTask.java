package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.tools.ant.util.JavaEnvUtils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;

/**
 * Created by manuel on 08.06.17.
 */
public class CompileJavaTask extends TestTask {
    private final File srcDir;
    private final File sourceFile;
    private final File destDir;
    private final String classPath;
    private final String commandArgs;


    public CompileJavaTask(File srcDir, File sourceFile, File destDir, String classPath, String commandArgs) {
        this.srcDir = srcDir;
        this.sourceFile = sourceFile;
        this.destDir = destDir;
        this.classPath = classPath;
        this.commandArgs = commandArgs;
    }

    @Override
    public String getDescription() {
        return "Compiling " + sourceFile;
    }

    @Override
    public void execute() throws Exception {
        destDir.mkdirs();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        String absoluteClassPath = makeAbsolute(classPath);
        absoluteClassPath = absoluteClassPath+":"+new File(JavaEnvUtils.getJavaHome()).getParent()+"/lib/tools.jar";

        String substCommandArgs = substituteVars(commandArgs, new String[] {"<classpath>", "<source>", "<file>", "<target>"}, new String[]{absoluteClassPath, srcDir.getAbsolutePath(), sourceFile.getAbsolutePath(), destDir.getAbsolutePath()});
        String[] compilerArgs = substCommandArgs.split(" ");
        compiler.run(null, null, null, compilerArgs);
    }


    public static String substituteVars(String str, String[] vars, String[] repl) {
        String ret = str;
        for (int i = 0; i < vars.length; i++){
            ret = ret.replaceAll(vars[i], repl[i]);
        }
        return ret;
    }

    public static String makeAbsolute(String classPath) {
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
