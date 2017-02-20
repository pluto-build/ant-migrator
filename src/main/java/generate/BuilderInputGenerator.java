package generate;

import generate.anthelpers.ReflectionHelpers;
import org.apache.tools.ant.*;
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
        this.addImport("build.pluto.output.Output");

        this.printString("public class " + name + " implements Serializable, Output, PlutoPropertyHelper.PropertySetter, Cloneable {", "}");
        this.increaseIndentation(1);

        this.generateClonableStructure();
        this.generatePropertySetter();

        this.printString("public String get(String v) {","}");
        this.increaseIndentation(1);
        this.printString("if (newProperties.containsKey(v))\n" +
                "  return newProperties.get(v);");
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
            "  return null;"
            );
        }
        this.closeOneLevel(); // end switch
        this.closeOneLevel(); // end method

        this.generateConfigureProjectMethod();

        this.printString("");
        this.generateNullOrEmptyMethod();
        this.generateEvalAsBooleanOrPropertyNameMethod();
        this.generateTestIfMethod();
        this.generateTestUnlessMethod();


        this.closeOneLevel(); // end class
    }

    private void generateClonableStructure() {
        this.addImport("java.util.HashMap");
        this.printString("private HashMap<String, String> newProperties = new HashMap<>();");

        this.printString("public " + name + "() { }");

        this.printString("private " + name + "(HashMap<String, String> properties) {", "}");
        this.increaseIndentation(1);
        this.printString("this.newProperties = properties;");
        this.closeOneLevel();

        this.printString("public " + name + " clone() {\n" +
                "  "+name+" clone = new "+name+"((HashMap<String, String>)newProperties.clone());\n" +
                "  return clone;\n" +
                "}");
    }

    public void generatePropertySetter() {
        this.printString("public void setProperty(String k, String v) {\n" +
                "  if (k != null && v != null)\n" +
                "    this.newProperties.put(k, v);\n" +
                "  }");
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
                } else if (!child.getTaskName().equals("property")) {
                    System.out.println("Encountered toplevel definition " + child.getTaskName() + " that didn't have an id. Don't know how to deal with that (yet).");
                }
            }
        }

        this.closeOneLevel();
    }

    private void generateNullOrEmptyMethod() {
        /*
        private static boolean nullOrEmpty(Object value) {
            return value == null || "".equals(value);
        }
         */

        this.printString("private boolean nullOrEmpty(Object v) {", "}");
        this.increaseIndentation(1);

        this.printString("return v == null || \"\".equals(v);");

        this.closeOneLevel(); // end method
    }

    private void generateEvalAsBooleanOrPropertyNameMethod() {
        this.addImport("org.apache.tools.ant.PropertyHelper");

        this.printString("private boolean evalAsBooleanOrPropertyName(Object v) {", "}");
        this.increaseIndentation(1);

        /*
            private boolean evalAsBooleanOrPropertyName(Object v) {
            Boolean b = toBoolean(v);
            if (b != null) {
                return b.booleanValue();
            }
            return this.get(String.valueOf(v)) != null;
        }
         */

        this.printString("Boolean b = PropertyHelper.toBoolean(v);");
        this.printString("if (b != null) {", "}");
        this.increaseIndentation(1);
        this.printString("return b.booleanValue();");
        this.closeOneLevel(); // end if

        this.printString("return this.get(String.valueOf(v)) != null;");

        this.closeOneLevel(); // end method
    }

    private void generateTestIfMethod() {
        this.addImport("org.apache.tools.ant.PropertyHelper");

        this.printString("public boolean testIf(Object v) {", "}");
        this.increaseIndentation(1);

        this.printString("return this.nullOrEmpty(v) || this.evalAsBooleanOrPropertyName(v);");

        this.closeOneLevel(); // end method
    }

    private void generateTestUnlessMethod() {
        this.addImport("org.apache.tools.ant.PropertyHelper");

        this.printString("public boolean testUnless(Object v) {", "}");
        this.increaseIndentation(1);

        this.printString("return this.nullOrEmpty(v) || !this.evalAsBooleanOrPropertyName(v);");

        this.closeOneLevel(); // end method
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
