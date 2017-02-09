package generate;

import generate.anthelpers.TargetReflectionHelpers;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Execute;
import utils.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by manuel on 13.12.16.
 */
public class BuilderInputGenerator extends Generator {

    private final String pkg;
    private final String name;
    private final Project project;
    private boolean includeEmpty = true;
    private PropertyResolver resolver;
    private NamingManager namingManager;

    public BuilderInputGenerator(String pkg, String name, Project project) {
        this.namingManager = new NamingManager();
        this.name = namingManager.getClassNameFor(StringUtils.capitalize(name));
        this.pkg = pkg;
        this.project = project;
        this.resolver = new PropertyResolver(project, "this");
    }

    /**
     *
     * @param name
     * @param includeEmpty set this to false, to omit empty variables in the output. This can result in wrong values, as empty variables can override environment variables...
     */
    public BuilderInputGenerator(String pkg, String name, Project project, boolean includeEmpty) {
        this(pkg, name, project);
        this.includeEmpty = includeEmpty;
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.printString("package " + pkg + ";");
        this.printString("import java.io.Serializable;");

        this.printString("public class " + name + " implements Serializable {", "}");
        this.increaseIndentation(1);

        this.printString("public String get(String v) {","}");
        this.increaseIndentation(1);
        this.printString("switch (v) {", "}");
        this.increaseIndentation(1);

        List<String> envPrefixes = getEnvPrefixes();
        project.getProperties().forEach(
                (k, v) -> {
                    if (!envPrefixes.stream().anyMatch(s ->
                            k.startsWith(s) && Execute.getEnvironmentVariables().containsKey(k.substring(s.length())))) {
                        this.printString("case \"" + k + "\":", "");
                        this.increaseIndentation(1);
                        this.printString("return " + resolver.getExpandedValue(StringUtils.javaPrint(v.toString())) + ";");
                        this.closeOneLevel();
                    }
                }
        );
        this.printString("default:\n" +
                "  // TODO: Remove prefix and check against environment prefixes...\n" +
                "  String envValue = System.getenv(v);\n" +
                "  if (envValue != null)\n" +
                "    return envValue;\n" +
                "  return \"\";");
        this.closeOneLevel();
        this.closeOneLevel();
        this.closeOneLevel();
    }

    public List<String> getEnvPrefixes() {
        List<String> result = new ArrayList<>();

        Target mainTarget = this.project.getTargets().get("");
        List<Object> children = TargetReflectionHelpers.getChildren(mainTarget);

        children.forEach(o ->  {
            if (o instanceof UnknownElement) {
                UnknownElement element = (UnknownElement)o;
                if (element.getTaskName().equals("property")) {
                    // We have a property. Check if it has an environment attribute
                    if (element.getWrapper().getAttributeMap().containsKey("environment")) {
                        String envPrefix = element.getWrapper().getAttributeMap().get("environment").toString();
                        if (!envPrefix.endsWith("."))
                            envPrefix = envPrefix + ".";
                        result.add(envPrefix);
                    }
                }
            }
        });

        return result;
    }
}
