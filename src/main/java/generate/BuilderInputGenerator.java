package generate;

import generate.anthelpers.ReflectionHelpers;
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
public class BuilderInputGenerator extends JavaGenerator {
    private final String name;
    private final Project project;
    private boolean includeEmpty = true;
    private PropertyResolver resolver;
    private NamingManager namingManager;
    private final ElementGenerator elementGenerator;

    public BuilderInputGenerator(String pkg, String name, Project project) {
        super(pkg);
        this.namingManager = new NamingManager();
        this.name = namingManager.getClassNameFor(StringUtils.capitalize(name));
        this.project = project;
        this.resolver = new PropertyResolver(project, "this");
        this.elementGenerator = new ElementGenerator(this, project, namingManager, resolver);
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

        this.addImport("java.io.Serializable");

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
        String envVars = "";
        for (String prefix : getEnvPrefixes()) {
            if (prefix.endsWith("."))
                prefix = prefix.substring(0, prefix.length()-1);
            envVars += "\"" + prefix + "\", ";
        }
        if (envVars.endsWith(", "))
            envVars = envVars.substring(0, envVars.length()-2);

        if (!envVars.isEmpty()) {
            this.addImport("java.util.List");
            this.addImport("java.util.Arrays");
            this.printString("default:\n" +
                    "  if (v.contains(\".\")) {\n" +
                    "    String prefix = v.substring(0, v.indexOf(\".\"));\n" +
                    "    String rest = v.substring(v.indexOf(\".\")+1);\n" +
                    "    List<String> envPrefixes = Arrays.asList(" + envVars + ");\n" +
                    "\n" +
                    "    if (envPrefixes.contains(prefix)) {\n" +
                    "      String envValue = System.getenv(rest);\n" +
                    "      if (envValue != null)\n" +
                    "        return envValue;\n" +
                    "    }\n" +
                    "  }\n" +
                    "  return \"\";");
        } else {
            this.printString("default:\n" +
            "  return \"\";"
            );
        }
        this.closeOneLevel(); // end switch
        this.closeOneLevel(); // end method

        this.generateConfigureProjectMethod();


        this.closeOneLevel(); // end class
    }

    private void generateConfigureProjectMethod() {
        this.addImport("org.apache.tools.ant.Project");
        this.printString("public void configureProject(Project project) {", "}");
        this.increaseIndentation(1);

        Target mainTarget = this.project.getTargets().get("");
        List<Object> children = ReflectionHelpers.getChildrenFor(mainTarget);

        for (Object o: children) {
            if (o instanceof UnknownElement) {
                UnknownElement child = (UnknownElement)o;

                if (child.getWrapper().getAttributeMap().containsKey("id")) {
                    String childName = namingManager.getNameFor(child.getTaskName());

                    elementGenerator.generateElement(childName, child, null, false);
                }
            }
        }

        this.closeOneLevel();
    }

    public List<String> getEnvPrefixes() {
        List<String> result = new ArrayList<>();

        Target mainTarget = this.project.getTargets().get("");
        List<Object> children = ReflectionHelpers.getChildrenFor(mainTarget);

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
