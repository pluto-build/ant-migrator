package generate;

import generate.introspectionhelpers.AntIntrospectionHelper;
import generate.introspectionhelpers.MacroAntIntrospectionHelper;
import generate.types.TTypeName;
import javafx.util.Pair;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import org.jetbrains.annotations.NotNull;
import utils.StringUtils;

import java.util.*;

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
    private final MacroAntIntrospectionHelper introspectionHelper;
    private final boolean continueOnError;

    public String getName() {
        return name;
    }

    public String getProjectName() {
        return namingManager.getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Input";
    }

    public MacroGenerator(String pkg, Project project, NamingManager namingManager, Resolvable resolver, UnknownElement macroDef, boolean continueOnError) {
        super(pkg + ".macros");
        this.basePkg = pkg;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.macroPropertyResolver = new MacroPropertyResolver(resolver);
        this.macroDef = macroDef;
        this.continueOnError = continueOnError;

        // The unknown element has to be a macro definition
        assert (this.macroDef.getTaskName().equals("macrodef"));

        String macroDefinitionName = this.macroDef.getWrapper().getAttributeMap().get("name").toString();
        String javaName = macroDefinitionName + "Macro";
        this.name = namingManager.getClassNameFor(javaName);
        this.inputName = namingManager.getClassNameFor(project.getName() + "Input");

        UnknownElement macroCallElement = new UnknownElement(macroDefinitionName);
        macroCallElement.setTaskName(macroDefinitionName);
        introspectionHelper = (MacroAntIntrospectionHelper)AntIntrospectionHelper.getInstanceFor(project, macroCallElement, macroDefinitionName, getPkg(), null);
    }

    @Override
    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        try {
            this.printString("public class " + this.name + " {", "}");
            this.increaseIndentation(1);

            for (UnknownElement child : this.macroDef.getChildren()) {
                prepareElement(child);
            }

            this.generateProject();

            for (UnknownElement child : this.macroDef.getChildren()) {
                generateElement(child);
            }

            this.generateExecuteMethod();

            this.closeOneLevel(); // end class
        } catch (Exception e) {
            if (!continueOnError)
                throw e;

            System.err.println("Failed to migrate macro: " + this.name);
            e.printStackTrace();
            this.printString("// TODO: Failed to migrate macro...");
        }
    }

    private void generateProject() {
        this.addImport("org.apache.tools.ant.Project");
        this.addImport(basePkg + "." + inputName);
        this.printString("final Project project;");
        this.printString("final " + inputName + " input;");
        this.printString("public Project getProject() {\n" +
                "  return this.project;\n" +
                "}");

        this.printString("public " + getName() + "(Project project, " + inputName + " input) {\n" +
                "  this.project = project;\n" +
                "  this.input = input;",
                "}");
        this.increaseIndentation(1);

        // Get the sequential element
        UnknownElement sequential = getSequential();


        this.printString(this.getInputName() + " cinput = input.clone();");

        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver, continueOnError);
        elementGenerator.setIgnoredMacroElements(definedElements);
        elementGenerator.setLocalScopedVariables(false);
        elementGenerator.setOnlyConstructors(true);

        for (UnknownElement child : sequential.getChildren()) {
            if (!definedElements.contains(child.getTaskName())) {
                AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(this.project, child, childNames.get(child), getPkg(), null);
                elementGenerator.generateElement(null, child, childNames.get(child));
            }
        }

        this.closeOneLevel(); // end method

        for (Map.Entry<UnknownElement, Pair<String, TTypeName>> entry : elementGenerator.getConstructedVariables().entrySet()) {
            childNames.put(entry.getKey(), entry.getValue().getKey());
            this.addImport(entry.getValue().getValue().getImportName());
            this.printString("private " + entry.getValue().getValue().getShortName() + " " + entry.getValue().getKey() + " = null;");
        }
    }

    HashMap<UnknownElement, String> childNames = new HashMap<>();
    List<String> definedElements = new ArrayList<>();

    private void generateExecuteMethod() {
        this.printString("public void execute() {", "}");
        this.increaseIndentation(1);

        // Get the sequential element
        UnknownElement sequential = getSequential();

        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver, continueOnError);
        elementGenerator.setIgnoredMacroElements(definedElements);
        elementGenerator.setLocalScopedVariables(false);
        elementGenerator.setNoConstructor(true);

        for (UnknownElement child : sequential.getChildren()) {
            // Implicits were already inserted above, do NOT repeat here!
            childNames.put(child, elementGenerator.generateElement(null, child, childNames.get(child), true));
        }

        for (UnknownElement child : sequential.getChildren()) {
            this.printString(childNames.get(child) + ".execute();");
        }

        this.closeOneLevel(); // end method
    }

    private void prepareElement(UnknownElement element) {
        if (element.getTaskName().equals("text")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String textName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));
            macroPropertyResolver.addAttribute(textName);
        }
        if (element.getTaskName().equals("attribute")) {
            String attributeName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));

            // TODO: probably fill these first to enable proper expansion
            // Remark: Probably not, as https://ant.apache.org/manual/Tasks/macrodef.html notes that order is important and expansion might not happen otherwise...
            macroPropertyResolver.addAttribute(attributeName);
        }
        if (element.getTaskName().equals("element")) {
            String elementName = element.getWrapper().getAttributeMap().get("name").toString();
            definedElements.add(elementName);

            String elementClassName = namingManager.getClassNameFor(elementName);

            UnknownElement sequential = getSequential();
            List<UnknownElement> parents = AntIntrospectionHelper.findParentsForNestedMacroElement(sequential, elementName);

            if (parents.isEmpty())
                throw new RuntimeException("Did not find <" + elementName + "/> element in macrodef.");

            for (UnknownElement parent : parents) {
                if (!childNames.containsKey(parent)) {
                    String parentName = namingManager.getNameFor(StringUtils.decapitalize(parent.getTaskName()));

                    childNames.put(parent, parentName);
                }
            }
        }
    }

    private void generateElement(UnknownElement element) {
        if (element.getTaskName().equals("text")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String textName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));
            this.printString("String " + textName + " = " + def + ";");

            this.printString("public void addText(String " + textName + ") {\n" +
                    "  this." + textName + " = " + textName + ";\n" +
                    "}");

            this.printString("public String get" + StringUtils.capitalize(textName) + "() {\n" +
                    "  return this." + textName + ";\n" +
                    "}");

            // TODO: optional, trim, description
        }
        if (element.getTaskName().equals("attribute")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String attributeName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));

            this.printString("String " + attributeName + " = " + def + ";");

            this.printString("public void set" + StringUtils.capitalize(attributeName) + "(String " + attributeName + ") {\n" +
                    "  this." + attributeName + " = " + attributeName + ";\n" +
                    "}");

            this.printString("public String get" + StringUtils.capitalize(attributeName) + "() {\n" +
                    "  return this." + attributeName + ";\n" +
                    "}");

            // TODO: doubleexpanding, description
        }
        if (element.getTaskName().equals("element")) {
            String elementName = element.getWrapper().getAttributeMap().get("name").toString();
            String elementClassName = namingManager.getClassNameFor(elementName);

            UnknownElement sequential = getSequential();
            List<UnknownElement> parents = AntIntrospectionHelper.findParentsForNestedMacroElement(sequential, elementName);

            if (parents.isEmpty())
                throw new RuntimeException("Did not find <" + elementName + "/> element in macrodef.");

            generateMacroElementClass(elementName, parents);

            this.printString("public " + namingManager.getClassNameFor(elementName) + " get" + namingManager.getClassNameFor(elementName) + "() { return new " + namingManager.getClassNameFor(elementName) + "(); }");

            // TODO: implict elements
        }
    }

    @NotNull
    private UnknownElement getSequential() {
        return introspectionHelper.getSequentialElement();
    }

    private void generateMacroElementClass(String elementName, List<UnknownElement> parents) {
        List<AntIntrospectionHelper> introspectionHelpers = introspectionHelper.getParentAntIntrospectionHelpers(elementName);
        List<String> commonSupportedNestedElements = MacroAntIntrospectionHelper.getCommonNestedElements(introspectionHelpers);
        this.printString("public class " + namingManager.getClassNameFor(elementName) + "{", "}");
        this.increaseIndentation(1);

        // Constructor
        this.printString("public " + namingManager.getClassNameFor(elementName) + "() { }");

        for (String nested : commonSupportedNestedElements) {
            // Currently assuming same type!
            TTypeName nestedType = introspectionHelpers.get(0).getNestedElementType(nested);
            UnknownElement nestedElement = new UnknownElement(nested);
            nestedElement.setTaskName(nested);

            this.addImport(nestedType.getImportName());
            this.addImport(basePkg + ".Consumer");
            this.printString("public void configure" + namingManager.getClassNameFor(nested) + "(final Consumer<" + nestedType.getShortName() + "> lam) {", "}");
            this.increaseIndentation(1);

            for (UnknownElement parent : parents) {
                //System.out.println(nested + ":" + parent.getTaskName());
                String nestedName = namingManager.getNameFor(StringUtils.decapitalize(nested));
                AntIntrospectionHelper parentIntrospectionHelper = introspectionHelpers.get(parents.indexOf(parent));
                // Very hacky...
                // We know better names than getParentAntIntrospectionHelpers can provide...
                parentIntrospectionHelper.setName(childNames.get(parent));
                AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(this.project, nestedElement, nestedName, getPkg(), parentIntrospectionHelper);
                TTypeName name = introspectionHelper.getElementTypeClassName();

                ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, resolver, continueOnError);

                elementGenerator.generateConstructor(introspectionHelper, nestedName);
                this.printString("lam.execute(" + nestedName + ");");
                elementGenerator.generateAddMethod(introspectionHelper, nestedName);

                //?
                elementGenerator.generateMacroInvocationSpecificCode(introspectionHelper);
            }

            this.closeOneLevel(); // end configure
        }

        this.closeOneLevel(); // end class
    }
}
