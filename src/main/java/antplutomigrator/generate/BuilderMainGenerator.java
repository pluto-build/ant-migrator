package antplutomigrator.generate;

import antplutomigrator.utils.StringUtils;

import java.util.List;

/**
 * Created by manuel on 06.02.17.
 */
public class BuilderMainGenerator extends JavaGenerator {

    private final String pkg;
    private final String name;
    private final List<String> defTargets;
    private final NamingManager namingManager;
    private final boolean useFileDependencyDiscovery;
    private final boolean enableDebugLogging;

    public BuilderMainGenerator(String pkg, String name, List<String> defTargets, boolean useFileDependencyDiscovery, boolean enableDebugLogging) {
        super(pkg);
        this.namingManager = new NamingManager();
        this.name = namingManager.getClassNameFor(StringUtils.capitalize(name));
        this.pkg = pkg;
        this.defTargets = defTargets;
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

        for (String defTarget: defTargets) {
            String contextName = namingManager.getNameFor("context");
            this.printString(StringUtils.capitalize(name) + "Context "+contextName+" = new " + StringUtils.capitalize(name) + "Context(\"" + StringUtils.capitalize(defTarget) + "\");");
            this.printString("BuildManagers.build(new BuildRequest<>(" + StringUtils.capitalize(defTarget) + "Builder.factory, "+contextName+"));");
        }

        if (useFileDependencyDiscovery) {
            this.addImport("build.pluto.tracing.TracingProvider");
            this.printString("TracingProvider.getTracer().stop();");
        }

        this.closeOneLevel();
        this.closeOneLevel();
    }
}