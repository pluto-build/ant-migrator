package generate;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import utils.ReflectionUtils;
import utils.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 29.11.16.
 */
public class BuilderGenerator extends Generator {
    private final String name;
    private final String pkg;
    private final Boolean useFileDependencyDiscovery;
    private List<String> dependentBuilders = new ArrayList<>();
    private List<String> dependentFiles = new ArrayList<>();
    private List<Task> commands = new ArrayList<>();
    private List<String> imports = new ArrayList<>();

    private NamingManager namingManager = new NamingManager();

    //<editor-fold desc="Getters and Setters" defaultstate="collapsed">
    public List<String> getDependentBuilders() {
        return dependentBuilders;
    }

    public void setDependentBuilders(List<String> dependentBuilders) {
        this.dependentBuilders = dependentBuilders;
    }

    public List<String> getDependentFiles() {
        return dependentFiles;
    }

    public void setDependentFiles(List<String> dependentFiles) {
        this.dependentFiles = dependentFiles;
    }

    public List<Task> getCommands() {
        return commands;
    }

    public void setCommands(List<Task> commands) {
        this.commands = commands;
    }

    public String getName() {
        return name;
    }

    public String getPkg() {
        return pkg;
    }

    public Boolean getUseFileDependencyDiscovery() {
        return useFileDependencyDiscovery;
    }

    public List<String> getImports() {
        return imports;
    }
    //</editor-fold>

    public BuilderGenerator(String pkg, String name, Boolean useFileDependencyDiscovery) {
        this.name = StringUtils.capitalize(name);
        this.useFileDependencyDiscovery = useFileDependencyDiscovery;
        this.pkg = pkg;
    }

    public void addImport(String cls) {
        if (!imports.contains(cls))
            imports.add(cls);
    }

    private void generateInputClass() {
        this.addImport("java.io.Serializable");
        this.addImport(getPkg() + "." + getName() + "." + getName() + "Input");
        this.printString("public static class " + getName() + "Input implements Serializable {\n" +
                "  //TODO\n" +
                "}");
    }

    private void generateBuildMethod() {
        this.addImport("java.io.IOException");
        this.printString("@Override\n" +
                "protected None build(" + getName() + "Input input) throws IOException {", "}");
        this.increaseIndentation(1);

        for (String fileDep : getDependentFiles()) {
            this.printString("require(new File(\"" + fileDep + "\"));");
        }

        for (String dep : getDependentBuilders()) {
            this.printString(StringUtils.capitalize(dep) + "Builder." + StringUtils.capitalize(dep) + "BuilderInput " + StringUtils.decapitalize(dep) + "BuilderInput = new " + StringUtils.capitalize(dep) + "Builder." + StringUtils.capitalize(dep) + "BuilderInput();");
            this.printString("requireBuild(" + StringUtils.capitalize(dep) + "Builder.factory, " + StringUtils.decapitalize(dep) + "BuilderInput);");
        }

        addImport("org.apache.tools.ant.Project");
        printString("Project project = new Project();");
        printString("project.addBuildListener(new PlutoBuildListener());");
        for (Task t : getCommands()) {
            if (t instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) t;
                String fullyQualifiedTaskdefName = "org.apache.tools.ant.taskdefs." + StringUtils.capitalize(t.getTaskName());
                addImport(fullyQualifiedTaskdefName);

                String taskClassName = StringUtils.capitalize(t.getTaskName());
                String taskName = namingManager.getNameFor(StringUtils.decapitalize(t.getTaskName()));

                this.printString(taskClassName + " " + taskName + " = new " + taskClassName + "();");
                this.printString(taskName + ".setProject(project);");
                element.getWrapper().getAttributeMap().forEach((n, o) ->
                        {
                            String setter = "set" + StringUtils.capitalize(n);
                            // Get type of argument
                            Class<?> argumentClass = String.class;
                            try {
                                Class<?> c = Class.forName(fullyQualifiedTaskdefName);
                                Method m = ReflectionUtils.getSetterForPorperty(c, n);
                                if (m != null && m.getParameterCount() > 0) {
                                    argumentClass = m.getParameterTypes()[0];
                                    setter = m.getName();
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }

                            String argument = "\"" + o.toString() + "\"";
                            if (argumentClass.getName().equals("boolean")) {
                                // We expect a boolean, use true or false as values without wrapping into a string.
                                argument = o.toString();
                            } else if (!argumentClass.getName().equals("java.lang.String")) {
                                // Not a string. Use single argument constructor from single string...
                                // This might not exist resulting in a type error in the resulting migrated Script
                                // TODO: Search for factory methods...
                                addImport(argumentClass.getName());
                                argument = "new " + argumentClass.getSimpleName() + "(\"" + o.toString() + "\")";
                            }

                            this.printString(taskName + "." + setter + "(" + argument + ");");
                        }
                );
                this.printString(taskName + ".execute();");
            } else {
                throw new RuntimeException("Didn't know how to handle " + t.toString());
                // TODO: Deal with non UnknownElements.
            }
            // TODO: task
        }

        this.printString("return None.val;");
        this.closeOneLevel();
    }

    private void generateClass() {
        this.addImport("build.pluto.builder.Builder");
        this.addImport("build.pluto.output.None");
        this.printString("public class " + getName() + " extends Builder<" + getName() + "Input, None> {", "}");
        this.increaseIndentation(1);
        this.addImport("build.pluto.builder.factory.BuilderFactory");
        this.addImport("build.pluto.builder.factory.BuilderFactoryFactory");
        this.printString("public static BuilderFactory<" + getName() + "Input, None, " + getName() + "> factory = BuilderFactoryFactory.of(" + getName() + ".class, " + getName() + "Input.class);");

        generateInputClass();

        this.printString("public " + getName() + "(" + getName() + "Input input) { super(input); }");

        this.printString("@Override\n" +
                "protected String description(" + getName() + "Input input) {\n" +
                "  return \"Builder " + getName() + ": \" + input;\n" +
                "}");

        this.addImport("java.io.File");
        this.printString("@Override\n" +
                "public File persistentPath(" + getName() + "Input input) {\n" +
                "  return new File(\"deps/" + getName() + ".dep\");\n" +
                "}");

        this.addImport("build.pluto.stamp.Stamper");
        this.addImport("build.pluto.stamp.FileHashStamper");
        this.printString("@Override\n" +
                "protected Stamper defaultStamper() {\n" +
                "  return FileHashStamper.instance;\n" +
                "}");


        generateBuildMethod();

        if (useFileDependencyDiscovery)
            generateUseFileDependencyDiscoveryPrettyPrint();
        this.closeOneLevel();
    }


    private void generateUseFileDependencyDiscoveryPrettyPrint() {
        this.printString("@Override\n" +
                "protected boolean useFileDependencyDiscovery() {\n" +
                "  return " + getUseFileDependencyDiscovery().toString() + ";\n" +
                "}");
    }

    private String getImportPrettyPrint() {
        StringBuilder sb = new StringBuilder();
        for (String anImport : getImports()) {
            sb.append("import ").append(anImport).append(";\n");
        }
        return sb.toString();
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();
        printString("package " + getPkg() + ";\n");
        printLater(() -> getImportPrettyPrint());
        this.printString("");
        generateClass();
    }
}
