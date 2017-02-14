package generate;

import org.apache.tools.ant.*;
import utils.ReflectionUtils;
import utils.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 29.11.16.
 */
public class BuilderGenerator extends JavaGenerator {
    private final String name;

    private final Project project;
    private final String projectName;
    private final Boolean useFileDependencyDiscovery;
    private List<String> dependentBuilders = new ArrayList<>();
    private List<String> dependentFiles = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();

    private final NamingManager namingManager = new NamingManager();
    private final PropertyResolver resolver;



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

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public String getName() {
        return name;
    }

    public Boolean getUseFileDependencyDiscovery() {
        return useFileDependencyDiscovery;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getInputName() {
        return getProjectName() + "Input";
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }
    //</editor-fold>

    public BuilderGenerator(String pkg, String name, Project project, Boolean useFileDependencyDiscovery) {
        super(pkg);
        this.name = getNamingManager().getClassNameFor(StringUtils.capitalize(name));
        this.project = project;
        this.projectName = getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
        this.useFileDependencyDiscovery = useFileDependencyDiscovery;
        this.resolver = new PropertyResolver(project, "input");
    }

    private void generateBuildMethod() {
        this.addImport("java.io.IOException");
        this.printString("@Override\n" +
                "protected None build(" + this.getInputName() + " input) throws IOException {", "}");
        this.increaseIndentation(1);

        for (String fileDep : getDependentFiles()) {
            this.printString("require(new File(\"" + fileDep + "\"));");
        }

        for (String dep : getDependentBuilders()) {
            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(dep));
            this.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            this.printString("requireBuild(" + depName + "Builder.factory, " + StringUtils.decapitalize(depName) + "Input);");
        }

        addImport("org.apache.tools.ant.Project");
        printString("Project project = new Project();");
        printString("project.addBuildListener(new PlutoBuildListener());");
        for (Task t : getTasks()) {
            if (t instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) t;

                String taskName = generateElement(element);

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

    private String generateElement(UnknownElement element) {

        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);

        AntTypeDefinition typeDefinition = componentHelper.getDefinition(element.getTaskName());

        String fullyQualifiedTaskdefName = typeDefinition.getClassName();
        addImport(fullyQualifiedTaskdefName);

        String taskClassName = fullyQualifiedTaskdefName.substring(fullyQualifiedTaskdefName.lastIndexOf(".")+1);
        String taskName = getNamingManager().getNameFor(StringUtils.decapitalize(element.getTaskName()));

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

                    String argument = StringUtils.javaPrint(o.toString());
                    if (argumentClass.getName().equals("boolean")) {
                        // We expect a boolean, use true or false as values without wrapping into a string.
                        argument = "Boolean.valueOf(\"" + resolver.getExpandedValue(o.toString()) + "\")";
                    } else if (!argumentClass.getName().equals("java.lang.String")) {
                        // Not a string. Use single argument constructor from single string...
                        // This might not exist resulting in a type error in the resulting migrated Script
                        // TODO: Search for factory methods...
                        addImport(argumentClass.getName());
                        argument = "new " + argumentClass.getSimpleName() + "(" + resolver.getExpandedValue(argument) + ")";
                    }

                    this.printString(taskName + "." + setter + "(" + resolver.getExpandedValue(argument) + ");");
                }
        );

        if (element.getChildren() != null) {
            element.getChildren().forEach(child ->
                {
                    String childName = generateElement(child);
                    this.printString(taskName + ".add(" + childName + ");");
                });
        }
        return taskName;
    }

    private void generateClass() {
        this.addImport("build.pluto.builder.Builder");
        this.addImport("build.pluto.output.None");
        this.printString("public class " + getName() + " extends Builder<" + this.getProjectName() + "Input, None> {", "}");
        this.increaseIndentation(1);
        this.addImport("build.pluto.builder.factory.BuilderFactory");
        this.addImport("build.pluto.builder.factory.BuilderFactoryFactory");
        this.printString("public static BuilderFactory<" + this.getProjectName() + "Input, None, " + getName() + "> factory = BuilderFactoryFactory.of(" + getName() + ".class, " + this.getProjectName() + "Input.class);");

        //generateInputClass();

        this.printString("public " + getName() + "(" + this.getProjectName() + "Input input) { super(input); }");

        this.printString("@Override\n" +
                "protected String description(" + this.getProjectName() + "Input input) {\n" +
                "  return \"Builder " + getName() + ": \" + input;\n" +
                "}");

        this.addImport("java.io.File");
        this.printString("@Override\n" +
                "public File persistentPath(" + this.getProjectName() + "Input input) {\n" +
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

    public void generatePrettyPrint() {
        super.generatePrettyPrint();
        generateClass();
    }
}
