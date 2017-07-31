package antplutomigrator.generate;

import antplutomigrator.utils.StringUtils;

/**
 * Created by manuel on 06.02.17.
 */
public class BuilderMainGenerator extends JavaGenerator {

    private final String pkg;
    private final String name;
    private final String defTarget;
    private final NamingManager namingManager;
    private final boolean useFileDependencyDiscovery;
    private final boolean enableDebugLogging;

    public BuilderMainGenerator(String pkg, String name, String defTarget, boolean useFileDependencyDiscovery, boolean enableDebugLogging) {
        super(pkg);
        this.namingManager = new NamingManager();
        this.name = namingManager.getClassNameFor(StringUtils.capitalize(name));
        this.pkg = pkg;
        this.defTarget = defTarget;
        this.useFileDependencyDiscovery = useFileDependencyDiscovery;
        this.enableDebugLogging = enableDebugLogging;
    }

    public String getName() {
        return name;
    }

    @Override
    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.addImport("build.pluto.builder.BuildManagers");
        this.addImport("build.pluto.builder.BuildRequest");
        if (enableDebugLogging)
            this.addImport("org.sugarj.common.Log");

        this.printString("public class "+ name +" {", "}");
        this.increaseIndentation(1);

        this.printString("public static void main(String[] args) throws Throwable {", "}");
        this.increaseIndentation(1);
        if (enableDebugLogging)
            this.printString("Log.log.setLoggingLevel(Log.ALWAYS);");
        this.printString(StringUtils.capitalize(name) + "Context context = new " + StringUtils.capitalize(name) + "Context(\""+StringUtils.capitalize(defTarget)+"\");");
        this.printString("BuildManagers.build(new BuildRequest<>("+StringUtils.capitalize(defTarget)+"Builder.factory, context));");

        if (useFileDependencyDiscovery) {
            this.addImport("build.pluto.tracing.TracingProvider");
            this.printString("TracingProvider.getTracer().stop();");
        }

        this.closeOneLevel();
        this.closeOneLevel();
    }
}