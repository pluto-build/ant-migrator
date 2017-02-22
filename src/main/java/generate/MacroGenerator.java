package generate;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 21.02.17.
 */
public class MacroGenerator extends JavaGenerator {

    private final Project project;
    private final NamingManager namingManager;
    private final Resolvable resolver;
    private final MacroPropertyResolver macroPropertyResolver;
    private final UnknownElement macroDef;
    private final String name;

    public String getName() {
        return name;
    }


    public MacroGenerator(String pkg, Project project, NamingManager namingManager, Resolvable resolver, UnknownElement macroDef) {
        super(pkg);
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.macroPropertyResolver = new MacroPropertyResolver(resolver);
        this.macroDef = macroDef;

        // The unknown element has to be a macro definition
        assert(this.macroDef.getTaskName().equals("macrodef"));

        String definedName = this.macroDef.getWrapper().getAttributeMap().get("name").toString() + "Macro";
        this.name = namingManager.getClassNameFor(definedName);
    }

    @Override
    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.printString("public class " + this.name + " {", "}");
        this.increaseIndentation(1);

        this.generateProject();

        for (UnknownElement child: this.macroDef.getChildren()) {
            generateElement(child);
        }

        this.generateExecuteMethod();


        this.closeOneLevel(); // end class
    }

    private void generateProject() {
        this.addImport("org.apache.tools.ant.Project");
        this.printString("Project project = null;");
        this.printString("public Project getProject() {" +
                "  return this.project;" +
                "}");
        this.printString("public void setProject(Project project) {" +
                "  this.project = project;" +
                "}");
    }

    private void generateExecuteMethod() {
        this.printString("public void execute() {","}");
        this.increaseIndentation(1);

        // TODO: Use more specialized resolver here!
        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver);

        // Get the sequential element
        UnknownElement sequential = macroDef.getChildren().stream().filter(element ->  element.getTaskName().equals("sequential")).findFirst().get();

        for (UnknownElement child: sequential.getChildren()) {
            String childName = namingManager.getNameFor(child.getTaskName());
            elementGenerator.generateElement(childName, child, null, false);

            this.printString(childName+".execute();");
        }

        this.closeOneLevel(); // end method
    }

    private void generateElement(UnknownElement element) {
        if (element.getTaskName().equals("text")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String textName = StringUtils.decapitalize(element.getWrapper().getAttributeMap().get("name").toString());
            macroPropertyResolver.addAttribute(textName);
            this.printString("String "+textName+" = " + def + ";");

            this.printString("public void addText(String "+textName+") {" +
                    "  this."+textName+" = "+textName+";" +
                    "}");

            this.printString("public String getText() {" +
                    "  return this."+textName+";" +
                    "}");

            // TODO: optional, trim, description
        }
        if (element.getTaskName().equals("attribute")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String attributeName = StringUtils.decapitalize(element.getWrapper().getAttributeMap().get("name").toString());

            // TODO: probably fill these first to enable proper expansion
            macroPropertyResolver.addAttribute(attributeName);

            this.printString("String "+attributeName+" = " + def + ";");

            this.printString("public void set"+StringUtils.capitalize(attributeName)+"(String "+attributeName+") {" +
                    "  this."+attributeName+" = "+attributeName+";" +
                    "}");

            this.printString("public String get"+StringUtils.capitalize(attributeName)+"() {" +
                    "  return this."+attributeName+";" +
                    "}");

            // TODO: doubleexpanding, description
        }
    }
}
