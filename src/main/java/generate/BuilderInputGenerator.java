package generate;

import generate.anthelpers.ReflectionHelpers;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Execute;
import utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

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
    private final File buildParent;
    private final boolean continueOnError;

    private final List<String> PATH_PROPERTY_NAMES;

    public BuilderInputGenerator(String pkg, String name, Project project, File buildParent, boolean continueOnError) {
        super(pkg);
        this.namingManager = new NamingManager();
        this.name = namingManager.getClassNameFor(StringUtils.capitalize(name+"Input"));
        this.project = project;
        this.resolver = new PropertyResolver(project, "this");
        this.buildParent = buildParent;
        this.elementGenerator = new ElementGenerator(this, project, namingManager, resolver, continueOnError);
        this.PATH_PROPERTY_NAMES = Arrays.asList("basedir", "ant.file." + name);
        this.continueOnError = continueOnError;
    }

    /**
     * @param name
     * @param includeEmpty set this to false, to omit empty variables in the output. This can result in wrong values, as empty variables can override environment variables...
     */
    public BuilderInputGenerator(String pkg, String name, Project project, File buildParent, boolean includeEmpty, boolean continueOnError) {
        this(pkg, name, project, buildParent, continueOnError);
        this.includeEmpty = includeEmpty;
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.addImport("java.io.Serializable");
        this.addImport("build.pluto.output.Output");

        this.printString("public class " + name + " implements Serializable, Output, PlutoPropertyHelper.PropertyInteractor, Cloneable {", "}");
        this.increaseIndentation(1);

        this.generateClonableStructure();
        this.generatePropertySetter();

        this.generateGetMethod();

        this.generateConfigureProjectMethod();

        this.printString("");
        this.generateNullOrEmptyMethod();
        this.generateEvalAsBooleanOrPropertyNameMethod();

        //this.generateRequireMethods();

        this.generateTestIfMethod();
        this.generateTestUnlessMethod();

        this.generateToStringMethod();
        this.generateEqualsHashcode();

        this.closeOneLevel(); // end class
    }

    private void generateGetMethod() {
        this.printString("public String get(String v) {", "}");
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
                        if (PATH_PROPERTY_NAMES.contains(k)) {
                            // Handle paths separately
                            String path = makeRelative(v.toString());
                            this.printString("return "+ resolver.getExpandedValue(StringUtils.javaPrint(path)) + ";");
                        } else {
                            this.printString("return " + resolver.getExpandedValue(StringUtils.javaPrint(v.toString())) + ";");
                        }
                        this.closeOneLevel();
                    }
                }
        );
        String envVars = "";
        for (String prefix : getEnvPrefixes()) {
            if (prefix.endsWith("."))
                prefix = prefix.substring(0, prefix.length() - 1);
            envVars += "\"" + prefix + "\", ";
        }
        if (envVars.endsWith(", "))
            envVars = envVars.substring(0, envVars.length() - 2);

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
                    "  return System.getProperty(v);");
        } else {
            this.printString("default:\n" +
                    "  return System.getProperty(v);"
            );
        }
        this.closeOneLevel(); // end switch
        this.closeOneLevel(); // end method
    }

    public String makeRelative(String path) {
        try {
            if (path.startsWith(buildParent.getCanonicalPath())) {
                path = path.replace(buildParent.getCanonicalPath(), ".");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    private void generateClonableStructure() {
        this.addImport("java.util.HashMap");
        this.printString("private String builderName;");
        this.printString("public String getBuilderName() {\n" +
                "  return this.builderName;\n" +
                "}");
        this.printString("private HashMap<String, String> newProperties = new HashMap<>();");

        this.printString("public " + name + "(String builderName) { this.builderName = builderName; }");

        this.printString("private " + name + "(String builderName, HashMap<String, String> properties) {", "}");
        this.increaseIndentation(1);
        this.printString("this.builderName = builderName;");
        this.printString("this.newProperties = properties;");
        this.closeOneLevel();

        this.printString("public " + name + " clone() {\n" +
                "  " + name + " clone = new " + name + "(builderName, (HashMap<String, String>)newProperties.clone());\n" +
                "  return clone;\n" +
                "}");

        this.printString("public " + name + " clone(String newBuilderName) {\n" +
                "  " + name + " clone = new " + name + "(newBuilderName, (HashMap<String, String>)newProperties.clone());\n" +
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

        this.printString("project.setBasedir(this.get(\"basedir\"));");

        Target mainTarget = this.project.getTargets().get("");
        List<Object> children = ReflectionHelpers.getChildrenFor(mainTarget);

        for (Object o : children) {
            if (o instanceof UnknownElement) {
                UnknownElement child = (UnknownElement) o;

                if (child.getWrapper().getAttributeMap().containsKey("id")) {
                    String childName = elementGenerator.generateElement(null, child, null);
                } else if (child.getTaskName().equals("macrodef")) {
                    // Deal with macros. First do macrodef execution to make them available everywhere

                    // This should have already been done by AntMigrator.java
                } else if (!child.getTaskName().equals("property")) {
                    System.err.println("Encountered toplevel definition \"" + child.getTaskName() + "\" that didn't have an id. Don't know how to deal with that (yet).");
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

    private void generateRequireMethods() {
        for (Target target: project.getTargets().values()) {
            if (!target.getName().isEmpty())
                generateRequireMethod(target);
        }
    }

    private void generateRequireMethod(Target target) {
        this.addImport("build.pluto.builder.Builder");
        this.addImport("java.io.IOException");
        this.printString("public " + name + " require"+namingManager.getClassNameFor(target.getName())+"Builder(Builder<"+name+","+name+"> builder) throws IOException {", "}");
        this.increaseIndentation(1);

        this.printString(name+" cinput = this.clone(\""+target.getName()+"\");");

        boolean hasCondition = false;
        if (target.getIf() != null && !target.getIf().isEmpty()) {
            this.printString("if (testIf(\""+resolver.getExpandedValue(target.getIf())+"\")) {", "}");
            this.increaseIndentation(1);

            hasCondition = true;
        }

        if (target.getUnless() != null && !target.getUnless().isEmpty()) {
            this.printString("if (testUnless(\""+resolver.getExpandedValue(target.getUnless())+"\")) {", "}");
            this.increaseIndentation(1);

            hasCondition = true;
        }

        // require the builder
        this.printString("builder.requireBuild("+namingManager.getClassNameFor(target.getName())+"Builder.factory, cinput);");

        if (hasCondition)
            this.closeOneLevel();

        this.printString("return cinput.clone(getBuilderName());");

        this.closeOneLevel();
    }

    private void generateToStringMethod() {
        this.printString("@Override\n" +
                "public String toString() {\n" +
                "  return this.builderName + \"@\" + this.hashCode() + \" \" + this.newProperties.toString() + \"\";\n" +
                "}");
    }

    private void generateEqualsHashcode() {
        this.printString("@Override\n" +
                "public boolean equals(Object o) {\n" +
                "  if (this == o) return true;\n" +
                "  if (o == null || getClass() != o.getClass()) return false;\n" +
                "\n" +
                "  "+this.name+" that = ("+this.name+") o;\n" +
                "\n" +
                "  if (!builderName.equals(that.builderName)) return false;\n" +
                "  return newProperties.equals(that.newProperties);\n" +
                "}\n" +
                "\n" +
                "@Override\n" +
                "public int hashCode() {\n" +
                "  int result = builderName.hashCode();\n" +
                "  result = 31 * result + newProperties.hashCode();\n" +
                "  return result;\n" +
                "}");
    }

    public List<String> getEnvPrefixes() {
        List<String> result = new ArrayList<>();

        Target mainTarget = this.project.getTargets().get("");
        List<Object> children = ReflectionHelpers.getChildrenFor(mainTarget);

        children.forEach(o -> {
            if (o instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) o;
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
