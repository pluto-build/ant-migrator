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
    private final String inputName;
    private final String basePkg;

    public String getName() {
        return name;
    }


    public MacroGenerator(String pkg, Project project, NamingManager namingManager, Resolvable resolver, UnknownElement macroDef) {
        super(pkg + ".macros");
        this.basePkg = pkg;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.macroPropertyResolver = new MacroPropertyResolver(resolver);
        this.macroDef = macroDef;

        // The unknown element has to be a macro definition
        assert(this.macroDef.getTaskName().equals("macrodef"));

        String definedName = this.macroDef.getWrapper().getAttributeMap().get("name").toString() + "Macro";
        this.name = namingManager.getClassNameFor(definedName);
        this.inputName = namingManager.getClassNameFor(project.getName() + "Input");
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
        this.addImport(basePkg + "." + inputName);
        this.printString("final Project project;");
        this.printString("final "+ inputName + " input;");
        this.printString("public Project getProject() {\n" +
                "  return this.project;\n" +
                "}");

        this.printString("public " + getName() + "(Project project, " + inputName + " input) {\n" +
                "  this.project = project;\n" +
                "  this.input = input;\n" +
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
            String childName = elementGenerator.generateElement(null, child);

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
            String textName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));
            macroPropertyResolver.addAttribute(textName);
            this.printString("String "+textName+" = " + def + ";");

            this.printString("public void addText(String "+textName+") {\n" +
                    "  this."+textName+" = "+textName+";\n" +
                    "}");

            this.printString("public String get"+StringUtils.capitalize(textName)+"() {\n" +
                    "  return this."+textName+";\n" +
                    "}");

            // TODO: optional, trim, description
        }
        if (element.getTaskName().equals("attribute")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String attributeName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));

            // TODO: probably fill these first to enable proper expansion
            // Remark: Probably not, as https://ant.apache.org/manual/Tasks/macrodef.html notes that order is important and expansion might not happen otherwise...
            macroPropertyResolver.addAttribute(attributeName);

            this.printString("String "+attributeName+" = " + def + ";");

            this.printString("public void set"+StringUtils.capitalize(attributeName)+"(String "+attributeName+") {\n" +
                    "  this."+attributeName+" = "+attributeName+";\n" +
                    "}");

            this.printString("public String get"+StringUtils.capitalize(attributeName)+"() {\n" +
                    "  return this."+attributeName+";\n" +
                    "}");

            // TODO: doubleexpanding, description
        }
    }
}
