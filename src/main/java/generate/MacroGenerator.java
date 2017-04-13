package generate;

import generate.introspectionhelpers.AntIntrospectionHelper;
import generate.introspectionhelpers.MacroAntIntrospectionHelper;
import generate.types.TTypeName;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import org.jetbrains.annotations.NotNull;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public String getName() {
        return name;
    }

    public String getProjectName() {
        return namingManager.getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Input";
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

        this.printString("public class " + this.name + " {", "}");
        this.increaseIndentation(1);

        this.generateProject();

        for (UnknownElement child : this.macroDef.getChildren()) {
            generateElement(child);
        }

        this.generatePrepareMethod();

        this.generateExecuteMethod();


        this.closeOneLevel(); // end class
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
                "  this.input = input;\n" +
                "}");
    }

    HashMap<UnknownElement, String> childNames = new HashMap<>();

    private void generatePrepareMethod() {
        // Get the sequential element
        UnknownElement sequential = getSequential();

        // Generate of the rest of toplevel definitions:
        for (UnknownElement child : sequential.getChildren()) {
            String childName;
            if (childNames.containsKey(child)) {
                childName = childNames.get(child);
            } else {
                childName = namingManager.getNameFor(StringUtils.decapitalize(child.getTaskName()));
                childNames.put(child, childName);
            }

            AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, child, childName, basePkg, null);
            this.printString("private " + introspectionHelper.getElementTypeClassName().getShortName() + " " + childName + " = null;");
        }

        this.printString("public void prepare() {", "}");
        this.increaseIndentation(1);

        this.printString(this.getInputName() + " cinput = input.clone();");

        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver);
        elementGenerator.setIgnoredMacroElements(definedElements);
        elementGenerator.setAlreadyDefinedNames(new ArrayList(childNames.values()));

        for (UnknownElement child : sequential.getChildren()) {
            childNames.put(child, elementGenerator.generateElement(null, child, childNames.get(child)));
        }

        this.closeOneLevel(); // end method
    }

    private void generateExecuteMethod() {
        this.printString("public void execute() {", "}");
        this.increaseIndentation(1);

        // Get the sequential element
        UnknownElement sequential = getSequential();

        for (UnknownElement child : sequential.getChildren()) {
            this.printString(childNames.get(child) + ".execute();");
        }

        this.closeOneLevel(); // end method
    }


    List<String> definedElements = new ArrayList<>();

    private void generateElement(UnknownElement element) {
        if (element.getTaskName().equals("text")) {
            String def = "null";
            if (element.getWrapper().getAttributeMap().containsKey("default")) {
                def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
            }
            String textName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));
            macroPropertyResolver.addAttribute(textName);
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

            // TODO: probably fill these first to enable proper expansion
            // Remark: Probably not, as https://ant.apache.org/manual/Tasks/macrodef.html notes that order is important and expansion might not happen otherwise...
            macroPropertyResolver.addAttribute(attributeName);

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
            definedElements.add(elementName);
            String elementClassName = namingManager.getClassNameFor(elementName);

            UnknownElement sequential = getSequential();
            List<UnknownElement> parents = AntIntrospectionHelper.findParentsForNestedElement(sequential, elementName);

            if (parents.isEmpty())
                throw new RuntimeException("Did not find <" + elementName + "/> element in macrodef.");

            for (UnknownElement parent : parents) {
                String parentName = namingManager.getNameFor(StringUtils.decapitalize(parent.getTaskName()));

                childNames.put(parent, parentName);
            }

            /*this.addImport(name.getImportName());
            this.printString("private " + name.getShortName() + " " + taskName + " = null;");
            this.printString("public " + name.getShortName() + " get" + elementClassName + "() {" , "}");
            this.increaseIndentation(1);

            this.printString("return " + taskName + ";");

            this.closeOneLevel();*/

            generateMacroElementClass(elementName, parents);

            this.printString("public " + namingManager.getClassNameFor(elementName) + " get" + namingManager.getClassNameFor(elementName) + "() { return new " + namingManager.getClassNameFor(elementName) + "(); }");

            // TODO: implict elements
        }
    }

    @NotNull
    private UnknownElement getSequential() {
        return macroDef.getChildren().stream().filter(e -> e.getTaskName().equals("sequential")).findFirst().get();
    }

    private void generateMacroElementClass(String elementName, List<UnknownElement> parents) {
        List<AntIntrospectionHelper> introspectionHelpers = parents.stream().map(parent -> {
            UnknownElement parentParent = AntIntrospectionHelper.findParentForNestedElement(this.macroDef, parent);
            AntIntrospectionHelper parentIntrospectionHelper;
            if (parentParent.equals(getSequential()))
                parentIntrospectionHelper = introspectionHelper;
            else
                parentIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(project, parentParent, parentParent.getTaskName(),getPkg(), null);
            return AntIntrospectionHelper.getInstanceFor(project, parent, childNames.get(parent), basePkg, parentIntrospectionHelper);
        }).collect(Collectors.toList());
        List<String> commonSupportedNestedElements = getCommonNestedElements(introspectionHelpers);
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
                System.out.println(nested + ":" + parent.getTaskName());
                String nestedName = namingManager.getNameFor(StringUtils.decapitalize(nested));
                AntIntrospectionHelper parentIntrospectionHelper = introspectionHelpers.get(parents.indexOf(parent));
                AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(this.project, nestedElement, nested, getPkg(), parentIntrospectionHelper);
                TTypeName name = introspectionHelper.getElementTypeClassName();

                ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, resolver);
                elementGenerator.setAlreadyDefinedNames(new ArrayList(childNames.values()));

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

    public List<String> getCommonNestedElements(List<AntIntrospectionHelper> introspectionHelpers) {
        assert (introspectionHelpers != null && introspectionHelpers.size() >= 1);
        List<AntIntrospectionHelper> helpers = new ArrayList<>();
        helpers.addAll(introspectionHelpers);
        List<String> commonSupportedNestedElements = helpers.get(0).getSupportedNestedElements();
        helpers.remove(0);
        for (AntIntrospectionHelper helper : helpers) {
            commonSupportedNestedElements.retainAll(helper.getSupportedNestedElements());
        }
        return commonSupportedNestedElements;
    }
}
