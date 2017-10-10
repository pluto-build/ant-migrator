package antplutomigrator.generate;

import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import antplutomigrator.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by manuel on 29.11.16.
 */
public class BuilderGenerator extends JavaGenerator {
    private final Log log = LogFactory.getLog(BuilderGenerator.class);

    private final String name;

    private final Project project;
    private final Boolean useFileDependencyDiscovery;
    private List<String> dependentFiles = new ArrayList<>();
    private final Target target;

    private final NamingManager namingManager = new NamingManager();
    private final PropertyResolver resolver;
    private final ElementGenerator elementGenerator;
    private final boolean continueOnError;


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
        return getProjectName() + "Context";
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public ElementGenerator getElementGenerator() {
        return elementGenerator;
    }

    //</editor-fold>

    public BuilderGenerator(String pkg, Project project, Target target, Boolean useFileDependencyDiscovery, boolean continueOnError) {
        super(pkg);
        this.name = getNamingManager().getClassNameFor(StringUtils.capitalize(target.getName() + "Builder"));
        this.project = project;
        this.target = target;
        this.useFileDependencyDiscovery = useFileDependencyDiscovery;
        this.resolver = new PropertyResolver(project, "context");
        this.elementGenerator = new ElementGenerator(this, project, getNamingManager(), resolver, continueOnError);
        this.continueOnError = continueOnError;
    }

    private void generateBuildMethod() {
        this.printString("@Override\n" +
                "protected "+getInputName()+" build(" + this.getInputName() + " ctx) throws Exception {", "}");
        this.increaseIndentation(1);


        for (String fileDep : getDependentFiles()) {
            this.printString("require(new File(\"" + fileDep + "\"));");
        }

        //this.printString("context = context.clone(\""+getName()+"\");");

        for (String dep : getDependentBuilders()) {
            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(dep));
            //this.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            this.printString("ctx = requireBuild(" + depName + "Builder.factory, ctx);");
            //this.printString("cinput = cinput.require"+depName+"Builder(this);");
        }

        this.printString("final "+this.getInputName()+" context = ctx.clone();");

        // Check for if and unless conditions:
        if (target.getIf() != null) {
            this.printString("if (!context.testIf(\"" + resolver.getExpandedValue(target.getIf()) + "\")) {", "}");
            this.increaseIndentation(1);
            this.printString("return context;");
            this.closeOneLevel();
        }
        if (target.getUnless() != null) {
            this.printString("if (!context.testUnless(\"" + resolver.getExpandedValue(target.getUnless()) + "\")) {", "}");
            this.increaseIndentation(1);
            this.printString("return context;");
            this.closeOneLevel();
        }

        for (Task t : target.getTasks()) {
            if (t instanceof UnknownElement) {
                UnknownElement element = (UnknownElement) t;

                // Generate code for the task, including all children in the build file
                getElementGenerator().generateElement(null, element, null);
            } else {
                // All tasks should also be UnknownElements. If not, fail the conversion
                throw new RuntimeException("Didn't know how to handle " + t.toString());
            }
        }

        this.printString("return context;");
        this.closeOneLevel();
    }


    private void generateClass() {
        this.addImport("build.pluto.builder.Builder");
        this.addImport("build.pluto.output.None");
        this.printString("public class " + getName() + " extends AntBuilder {", "}");
        this.increaseIndentation(1);
        this.addImport("build.pluto.builder.factory.BuilderFactory");
        this.addImport("build.pluto.builder.factory.BuilderFactoryFactory");
        this.printString("public static BuilderFactory<" + this.getProjectName() + "Context, " + this.getProjectName() + "Context, " + getName() + "> factory = BuilderFactoryFactory.of("+this.getName()+".class, " + this.getProjectName() + "Context.class);");

        this.printString("public " + getName() + "(" + this.getProjectName() + "Context context) { super(context); }");

        /*this.printString("@Override\n" +
                "protected String description(" + this.getProjectName() + "Context context) {\n" +
                "  return \"Builder " + getName() + ": \" + context;\n" +
                "}");

        this.addImport("java.io.File");
        this.printString("@Override\n" +
                "public File persistentPath(" + this.getProjectName() + "Context context) {\n" +
                "  return new File(\"deps/" + getName() + ".dep\");\n" +
                "}");

        this.addImport("build.pluto.stamp.Stamper");
        this.addImport("build.pluto.stamp.FileHashStamper");
        this.printString("@Override\n" +
                "protected Stamper defaultStamper() {\n" +
                "  return FileHashStamper.instance;\n" +
                "}");*/


        generateBuildMethod();

        if (useFileDependencyDiscovery)
            generateUseFileDependencyDiscoveryPrettyPrint();
        this.closeOneLevel();
    }


    private void generateUseFileDependencyDiscoveryPrettyPrint() {
        /*this.printString("@Override\n" +
                "protected boolean useFileDependencyDiscovery() {\n" +
                "  return " + getUseFileDependencyDiscovery().toString() + ";\n" +
                "}");*/
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();
        generateClass();
    }
}
