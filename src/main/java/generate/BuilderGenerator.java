package generate;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by manuel on 29.11.16.
 */
public class BuilderGenerator extends JavaGenerator {
    private final String name;

    private final Project project;
    private final Boolean useFileDependencyDiscovery;
    private List<String> dependentFiles = new ArrayList<>();
    private final Target target;

    private final NamingManager namingManager = new NamingManager();
    private final PropertyResolver resolver;
    private final ElementGenerator elementGenerator;


    //<editor-fold desc="Getters and Setters" defaultstate="collapsed">
    public List<String> getDependentBuilders() {
        return Collections.list(target.getDependencies());
    }

    public List<String> getDependentFiles() {
        return dependentFiles;
    }

    public void setDependentFiles(List<String> dependentFiles) {
        this.dependentFiles = dependentFiles;
    }

    public Target getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public Boolean getUseFileDependencyDiscovery() {
        return useFileDependencyDiscovery;
    }

    public String getProjectName() {
        return getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Input";
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public ElementGenerator getElementGenerator() {
        return elementGenerator;
    }

    //</editor-fold>

    public BuilderGenerator(String pkg, Project project, Target target, Boolean useFileDependencyDiscovery) {
        super(pkg);
        this.name = getNamingManager().getClassNameFor(StringUtils.capitalize(target.getName() + "Builder"));
        this.project = project;
        this.target = target;
        this.useFileDependencyDiscovery = useFileDependencyDiscovery;
        this.resolver = new PropertyResolver(project, "cinput");
        this.elementGenerator = new ElementGenerator(this, project, getNamingManager(), resolver);
    }

    private void generateBuildMethod() {
        this.printString("@Override\n" +
                "protected "+getInputName()+" build(" + this.getInputName() + " input) throws Exception {", "}");
        this.increaseIndentation(1);

        this.printString(this.getInputName() + " cinput = input.clone();");

        for (String fileDep : getDependentFiles()) {
            this.printString("require(new File(\"" + fileDep + "\"));");
        }

        for (String dep : getDependentBuilders()) {
            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(dep));
            //this.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            this.printString("cinput = requireBuild(" + depName + "Builder.factory, cinput.clone());");
        }

        // Check for if and unless conditions:
        if (target.getIf() != null) {
            this.printString("if (!cinput.testIf(\"" + resolver.getExpandedValue(target.getIf()) + "\")) {", "}");
            this.increaseIndentation(1);
            this.printString("return cinput.clone();");
            this.closeOneLevel();
        }
        if (target.getUnless() != null) {
            this.printString("if (!input.testUnless(\"" + resolver.getExpandedValue(target.getUnless()) + "\")) {", "}");
            this.increaseIndentation(1);
            this.printString("return cinput.clone();");
            this.closeOneLevel();
        }

        addImport("org.apache.tools.ant.Project");
        printString("Project project = new Project();");
        printString("project.addBuildListener(new PlutoBuildListener());");
        printString("cinput.configureProject(project);");
        printString("PlutoPropertyHelper propertyHelper = PlutoPropertyHelper.getPropertyHelper(project);");
        printString("propertyHelper.setPropertySetter(cinput);");
        for (Task t : target.getTasks()) {
            if (t instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) t;

                // Create unique name for the task
                String taskName = getNamingManager().getNameFor(StringUtils.decapitalize(element.getTaskName()));

                // Generate code for the task, including all children in the build file
                getElementGenerator().generateElement(taskName, element, null, false);

                // Antcalls are resolved directly to builder calls. No calling of execute...
                if (!element.getTaskName().equals("antcall"))
                    this.printString(taskName + ".execute();");
            } else {
                // All tasks should also be UnknownElements. If not, fail the conversion
                throw new RuntimeException("Didn't know how to handle " + t.toString());
            }
        }

        this.printString("return cinput.clone();");
        this.closeOneLevel();
    }


    private void generateClass() {
        this.addImport("build.pluto.builder.Builder");
        this.addImport("build.pluto.output.None");
        this.printString("public class " + getName() + " extends Builder<" + this.getProjectName() + "Input, " + this.getProjectName() + "Input> {", "}");
        this.increaseIndentation(1);
        this.addImport("build.pluto.builder.factory.BuilderFactory");
        this.addImport("build.pluto.builder.factory.BuilderFactoryFactory");
        this.printString("public static BuilderFactory<" + this.getProjectName() + "Input, " + this.getProjectName() + "Input, " + getName() + "> factory = BuilderFactoryFactory.of(" + getName() + ".class, " + this.getProjectName() + "Input.class);");

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
