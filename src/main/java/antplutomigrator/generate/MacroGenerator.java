package antplutomigrator.generate;

import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.introspectionhelpers.MacroAntIntrospectionHelper;
import antplutomigrator.generate.transformers.ConfigureTaskTransformer;
import antplutomigrator.generate.transformers.ConstructorTaskTransformer;
import antplutomigrator.generate.types.TTypeName;
import javafx.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import org.jetbrains.annotations.NotNull;
import antplutomigrator.utils.StringUtils;

import java.util.*;

/**
 * Created by manuel on 21.02.17.
 */
public class MacroGenerator extends JavaGenerator {

    private Log log = LogFactory.getLog(MacroGenerator.class);

    private final Project project;
    private final NamingManager namingManager;
    private final Resolvable resolver;
    private final MacroPropertyResolver macroPropertyResolver;
    private final UnknownElement macroDef;
    private final String name;
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
        return namingManager.getClassNameFor(getProjectName() + "Context");
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

        UnknownElement macroCallElement = new UnknownElement(macroDefinitionName);
        macroCallElement.setTaskName(macroDefinitionName);
        introspectionHelper = (MacroAntIntrospectionHelper)AntIntrospectionHelper.getInstanceFor(project, macroCallElement, macroDefinitionName, getPkg(), null);
    }

    @Override
    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        try {
            log.trace("Generating macro: " + this.getName());
            this.addImport("org.apache.tools.ant.Task");
            this.printString("public class " + this.name + " extends Task {", "}");
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

            log.error("Failed to migrate macro: " + this.name, e);
            this.printString("// TODO: Failed to migrate macro...");

            this.closeOneLevel();
        }
    }

    private void generateProject() {
        this.addImport("org.apache.tools.ant.Project");
        this.addImport(basePkg + "." + getInputName());
        this.printString("final " + getInputName() + " context;");
        this.addImport(basePkg + ".AntBuilder");
        this.printString("final AntBuilder builder;");

        this.printString("public " + getName() + "(AntBuilder builder, " + getInputName() + " context) {\n" +
                "  this.builder = builder;\n" +
                "  this.context = context;",
                "}");
        this.increaseIndentation(1);

        // Get the sequential element
        UnknownElement sequential = getSequential();


        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver, continueOnError);
        elementGenerator.setIgnoredMacroElements(definedElements);
        elementGenerator.setLocalScopedVariables(false);
        elementGenerator.setOnlyConstructors(true);
        elementGenerator.setInMacro(true);

        for (UnknownElement child : sequential.getChildren()) {
            if (!definedElements.contains(child.getTaskName())) {
                elementGenerator.generateElement(null, child, null);
            }
        }

        // Initialize defaults for attributes
        for (UnknownElement element : this.macroDef.getChildren()) {
            if (element.getTaskName().equals("attribute")) {
                String def = "null";
                if (element.getWrapper().getAttributeMap().containsKey("default")) {
                    def = "\"" + macroPropertyResolver.getExpandedValue(resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("default").toString())) + "\"";
                }
                String attributeName = namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()).toLowerCase();

                this.printString(attributeName + " = " + def + ";");
            }
        }

        this.closeOneLevel(); // end method

        for (Map.Entry<UnknownElement, Pair<String, TTypeName>> entry : elementGenerator.getConstructedVariables().entrySet()) {
            this.addImport(entry.getValue().getValue().getImportName());
            this.printString("private " + entry.getValue().getValue().getShortName() + " " + entry.getValue().getKey() + " = null;");
        }
    }

    List<String> definedElements = new ArrayList<>();

    private void generateExecuteMethod() {
        this.printString("@Override");
        this.printString("public void execute() {", "}");
        this.increaseIndentation(1);

        // Get the sequential element
        UnknownElement sequential = getSequential();

        ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver, continueOnError);
        elementGenerator.setIgnoredMacroElements(definedElements);
        elementGenerator.setLocalScopedVariables(false);
        elementGenerator.setNoConstructor(true);
        elementGenerator.setInMacro(true);

        for (UnknownElement child : sequential.getChildren()) {
            // Implicits were already inserted above, do NOT repeat here!
            elementGenerator.generateElement(null, child, null, true);
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
            macroPropertyResolver.addAttribute(element.getWrapper().getAttributeMap().get("name").toString());
        }
        if (element.getTaskName().equals("attribute")) {
            String attributeName = StringUtils.decapitalize(namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()));

            // TODO: probably fill these first to enable proper expansion
            // Remark: Probably not, as https://ant.apache.org/manual/Tasks/macrodef.html notes that order is important and expansion might not happen otherwise...
            macroPropertyResolver.addAttribute(element.getWrapper().getAttributeMap().get("name").toString());
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
                namingManager.getNameFor(parent);
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
            log.trace("Generating macro text element: " + textName);
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
            String attributeName = namingManager.getClassNameFor(element.getWrapper().getAttributeMap().get("name").toString()).toLowerCase();

            log.trace("Generating macro attribute: " + attributeName);

            this.printString("String " + attributeName + ";");

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

            log.trace("Generating macro child element: " + elementName);

            UnknownElement sequential = getSequential();
            List<UnknownElement> parents = AntIntrospectionHelper.findParentsForNestedMacroElement(sequential, elementName);

            if (parents.isEmpty())
                throw new RuntimeException("Did not find <" + elementName + "/> element in macrodef.");

            generateMacroElementClass(elementName, parents);

            this.printString("public " + namingManager.getClassNameFor(elementName) + " get" + namingManager.getClassNameFor(elementName) + "() { return new " + namingManager.getClassNameFor(elementName) + "(); }");
        }
    }

    @NotNull
    private UnknownElement getSequential() {
        return introspectionHelper.getSequentialElement();
    }

    private void generateMacroElementClass(String elementName, List<UnknownElement> parents) {
        List<AntIntrospectionHelper> introspectionHelpers = introspectionHelper.getParentAntIntrospectionHelpers(elementName);
        List<String> commonSupportedNestedElements = MacroAntIntrospectionHelper.getCommonNestedElements(introspectionHelpers);
        this.printString("public class " + namingManager.getClassNameFor(elementName) + " {", "}");
        this.increaseIndentation(1);

        // Constructor
        this.printString("public " + namingManager.getClassNameFor(elementName) + "() { }");

        for (String nested : commonSupportedNestedElements) {
            // Currently assuming same type!
            TTypeName nestedType = introspectionHelpers.get(0).getNestedElementType(nested);
            UnknownElement nestedElement = new UnknownElement(nested);
            nestedElement.setTaskName(nested);

            this.addImport(nestedType.getImportName());
            this.addImport(basePkg + ".BiConsumer");
            this.printString("public void configure" + namingManager.getClassNameFor(nested) + "(final BiConsumer<" + nestedType.getShortName() + ", "+ getInputName() +"> lam) {", "}");
            this.increaseIndentation(1);

            for (UnknownElement parent : parents) {
                //System.out.println(nested + ":" + parent.getTaskName());
                String nestedName = namingManager.getNameFor(StringUtils.decapitalize(nested));
                AntIntrospectionHelper parentIntrospectionHelper = introspectionHelpers.get(parents.indexOf(parent));
                // Very hacky...
                // We know better names than getParentAntIntrospectionHelpers can provide...
                parentIntrospectionHelper.setName(namingManager.getNameFor(parent));
                AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(this.project, nestedElement, nestedName, getPkg(), parentIntrospectionHelper);
                //TTypeName name = introspectionHelper.getElementTypeClassName();

                ElementGenerator elementGenerator = new ElementGenerator(this, project, namingManager, macroPropertyResolver, continueOnError);

                ConstructorTaskTransformer constructorTaskTransformer = new ConstructorTaskTransformer(nestedElement,  elementGenerator, introspectionHelper);
                constructorTaskTransformer.transform();
                this.printString("lam.execute(" + nestedName + ", context);");
                ConfigureTaskTransformer configureTaskTransformer = new ConfigureTaskTransformer(nestedElement, elementGenerator, introspectionHelper);
                configureTaskTransformer.generateAddMethod(introspectionHelper, nestedName);

                //TODO ?
                //elementGenerator.generateMacroInvocationSpecificCode(introspectionHelper);
            }

            this.closeOneLevel(); // end configure
        }

        this.closeOneLevel(); // end class
    }
}
