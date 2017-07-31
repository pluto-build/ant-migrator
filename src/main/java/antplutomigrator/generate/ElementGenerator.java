package antplutomigrator.generate;

import antplutomigrator.generate.anthelpers.ReflectionHelpers;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.types.TConstructor;
import antplutomigrator.generate.types.TMethod;
import antplutomigrator.generate.types.TParameter;
import antplutomigrator.generate.types.TTypeName;
import javafx.util.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.EnumeratedAttribute;
import antplutomigrator.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by manuel on 16.02.17.
 */
public class ElementGenerator {

    private Log log = LogFactory.getLog(ElementGenerator.class);

    private final JavaGenerator generator;
    private final Project project;
    private final NamingManager namingManager;
    private final Resolvable resolver;
    private List<String> ignoredMacroElements = new ArrayList<>();
    private Map<UnknownElement, Pair<String, TTypeName>> constructedVariables = new HashMap<>();
    private boolean localScopedVariables = true;
    private boolean noConstructor = false;
    private boolean onlyConstructors = false;
    private final boolean continueOnErrors;
    private final String contextName;

    public List<String> getIgnoredMacroElements() {
        return ignoredMacroElements;
    }

    public void setIgnoredMacroElements(List<String> ignoredMacroElements) {
        this.ignoredMacroElements = ignoredMacroElements;
    }

    public Map<UnknownElement, Pair<String, TTypeName>> getConstructedVariables() {
        return constructedVariables;
    }
    public void setLocalScopedVariables(boolean localScopedVariables) {
        this.localScopedVariables = localScopedVariables;
    }

    public void setNoConstructor(boolean noConstructor) {
        this.noConstructor = noConstructor;
    }

    public void setOnlyConstructors(boolean onlyConstructors) {
        this.onlyConstructors = onlyConstructors;
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public String getProjectName() {
        return getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Context";
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, Resolvable resolver, boolean continueOnErrors) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.continueOnErrors = continueOnErrors;
        this.contextName = "context";
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, Resolvable resolver, boolean continueOnErrors, String contextName) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.continueOnErrors = continueOnErrors;
        this.contextName = contextName;
    }

    public String generateElement(AntIntrospectionHelper parentIntrospectionHelper, UnknownElement element, String taskName) {
        return generateElement(parentIntrospectionHelper, element, taskName, false);
    }

    public String generateElement(AntIntrospectionHelper parentIntrospectionHelper, UnknownElement element, String taskName, boolean implicitInserted) {
        try {
            log.trace("Generating element: " + element.getTaskName() + " at " + element.getLocation().toString());

            // Search for implicit elements and insert them explicitly...
            if (!implicitInserted)
                insertImplicitElements(element, parentIntrospectionHelper);

            // macros were already migrated...
            if (element.getTaskName().equals("macrodef"))
                return taskName;

            if (taskName == null)
                taskName = getNamingManager().getNameFor(element);

            if (ignoredMacroElements.contains(element.getTaskName())) {
                if (element.getChildren() == null || element.getChildren().isEmpty())
                    return taskName;
            }

            AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, taskName, generator.getPkg(), parentIntrospectionHelper);

            if (!onlyConstructors) {
                if (introspectionHelper.isAntCall()) {
                    // Deal with antcalls
                    generateAntCall(introspectionHelper);

                    return taskName;
                }
            }

            TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();
            if (elementTypeClassName == null)
                throw new RuntimeException("Could not get type definition for " + element.getTaskName());

            if (!noConstructor)
                generateConstructor(introspectionHelper, taskName);

            if (!onlyConstructors) {
                if (introspectionHelper.hasProjectSetter())
                    generateProjectSetter(taskName);

                if (introspectionHelper.hasInitMethod())
                    generator.printString(taskName + ".init();");
                // TODO: Do this right...
                //generator.printString(contextName+".initTask("+taskName+");");

                try {
                    // Just for debugging purposes right now
                    // TODO: Remove in release
                    element.maybeConfigure();
                } catch (Throwable t) {

                }
            }

            if (!onlyConstructors) {
                generateAttributes(element, taskName, introspectionHelper);

                // Element might include text. Call addText method...
                generateText(element, taskName);
            }

            generateChildren(element, taskName, introspectionHelper);

            if (!onlyConstructors) {
                generateMacroInvocationSpecificCode(introspectionHelper);
            }
        } catch (Exception e) {
            if (!continueOnErrors)
                throw e;
            log.error("Failed generating element: " + element.getTaskName() + " at " + element.getLocation().toString(), e);
            generator.printString("// TODO: Error while migrating " + element.getTaskName() + " at " + element.getLocation().toString());
            if (element.getChildren() != null) {
                generator.printString("// all children will also not be generated...");
                for (UnknownElement c: element.getChildren()) {
                    log.error("-->  Failed generating child: " + c.getTaskName() + " at " + c.getLocation().toString());
                    generator.printString("//  -->  Failed generating element: " + c.getTaskName() + " at " + c.getLocation().toString());
                }
            }
        }
        return taskName;
    }

    public void insertImplicitElements(UnknownElement element, AntIntrospectionHelper parentIntrospectionHelper) {
        AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, element.getTaskName(), generator.getPkg(), parentIntrospectionHelper);

        if (introspectionHelper.hasImplicitElement()) {

            log.debug("Inserting implicit element " + introspectionHelper.getImplicitElementName() + " into " + element.getTaskName() + " at " + element.getLocation());
            UnknownElement implicitElement = new UnknownElement(introspectionHelper.getImplicitElementName());
            implicitElement.setTaskName(introspectionHelper.getImplicitElementName());
            implicitElement.setRuntimeConfigurableWrapper(new RuntimeConfigurable(implicitElement, introspectionHelper.getImplicitElementName()));
            implicitElement.setLocation(element.getLocation());
            for (UnknownElement child: element.getChildren()) {
                insertImplicitElements(child, introspectionHelper);
                implicitElement.addChild(child);
            }

            ReflectionHelpers.clearChildrenFor(element);
            element.addChild(implicitElement);
        }
    }

    public void generateMacroInvocationSpecificCode(AntIntrospectionHelper introspectionHelper) {
        // Close lambda
        if (introspectionHelper.getParentIntrospectionHelper()!= null && introspectionHelper.getParentIntrospectionHelper().isMacroInvocationChildElement()) {
            generator.closeOneLevel();
            generator.closeOneLevel();
        }
    }

    public void generateChildren(UnknownElement element, String taskName, AntIntrospectionHelper introspectionHelper) {
        if (element.getChildren() != null) {
            for (UnknownElement child : element.getChildren()) {
                generator.increaseIndentation(1);
                if (introspectionHelper.supportsNestedElement(child.getTaskName())) {
                    generateElement(introspectionHelper, child, null, true);
                } else {
                    Class<?> elementTypeClass = introspectionHelper.getElementTypeClass();
                    if (elementTypeClass == null || !(TaskContainer.class.isAssignableFrom(elementTypeClass))) {
                        // Ignore macro child elements at definition...
                        if (!getIgnoredMacroElements().contains(child.getTaskName()))
                            throw new RuntimeException("Didn't support nested element: " + child.getTaskName());
                    } else {
                        // a task container - anything could happen - just add the
                        // child to the container
                        String childName = generateElement(introspectionHelper, child, null, true);
                        generator.printString(taskName + ".addTask(" + childName + ");");
                    }

                }
                generator.increaseIndentation(-1);
            }
        }

        generateAddMethod(introspectionHelper, taskName);
    }

    public void generateAddMethod(AntIntrospectionHelper introspectionHelper, String taskName) {
        if (introspectionHelper.getAddChildMethod() != null) {
            generator.printString(introspectionHelper.getParentIntrospectionHelper().getName() + "." + introspectionHelper.getAddChildMethod().getName() + "(" + taskName + ");");
        }
    }

    public void generateText(UnknownElement element, String taskName) {
        String text = element.getWrapper().getText().toString();
        if (!text.trim().isEmpty()) {
            text = resolver.getExpandedValue(StringEscapeUtils.escapeJava(text));
            generator.printString(taskName + ".addText(\"" + text + "\");");
        }
    }

    public void generateAttributes(UnknownElement element, String taskName, AntIntrospectionHelper introspectionHelper) {
        for (Map.Entry<String, Object> entry : introspectionHelper.getAttributeMap().entrySet()) {
            String n = entry.getKey().toLowerCase();
            Object o = entry.getValue();

            if (n.equals("id")) {
                // We have a reference id. Add code to add it to the project.
                generator.printString(getProject()+".addReference(\"" + element.getWrapper().getAttributeMap().get("id") + "\", " + taskName + ");");
                continue;
            }

            TMethod setterMethod = introspectionHelper.getAttributeMethod(n);

            // Get type of argument
            Class<?> argumentClass = introspectionHelper.getAttributeMethodType(n.toLowerCase());

            final String escapedValue = resolver.getExpandedValue(StringEscapeUtils.escapeJava(o.toString()));
            String argument = "\"" + escapedValue + "\"";
            if (argumentClass.getName().equals("boolean")) {
                // We expect a boolean, use true or false as values without wrapping into a string.
                generator.addImport("org.apache.tools.ant.Project");
                argument = "Project.toBoolean(\"" + escapedValue + "\")";
            } else if (java.io.File.class.equals(argumentClass)) {
                generator.addImport("org.apache.tools.ant.Project");
                argument = contextName+".resolveFile(\"" + escapedValue + "\")";
            } else if (EnumeratedAttribute.class.isAssignableFrom(argumentClass)) {
                TTypeName argumentClassName = new TTypeName(argumentClass.getName());
                String shortName = argumentClassName.getShortName();
                String attrName = getNamingManager().getNameFor(StringUtils.decapitalize(shortName));
                generator.addImport(argumentClassName.getImportName());
                generator.printString(shortName + " " + attrName + " = new " + shortName + "();");
                generator.printString(attrName + ".setValue(\"" + escapedValue + "\");");
                argument = attrName;
            } else if (argumentClass.getTypeName().equals("int")) {
                argument = "Integer.parseInt(\"" + o.toString() + "\")";

            } else if (argumentClass.getTypeName().equals("long")) {
                argument = "Long.parseLong(\"" + o.toString() + "\")";

            } else if (!(argumentClass.getName().equals("java.lang.String") || argumentClass.getName().equals("java.lang.Object"))) {

                boolean includeProject;
                Constructor<?> c;
                try {
                    // First try with Project.
                    c = argumentClass.getConstructor(Project.class, String.class);
                    includeProject = true;
                } catch (final NoSuchMethodException nme) {
                    // OK, try without.
                    try {
                        c = argumentClass.getConstructor(String.class);
                        includeProject = false;
                    } catch (final NoSuchMethodException nme2) {
                        // Well, no matching constructor.
                        throw new RuntimeException("We didn't find any matching constructor for type " + argumentClass.toString());
                    }
                }

                generator.addImport(argumentClass.getName());

                // Not a string. Use single argument constructor from single string...
                // This might not exist resulting in a type error in the resulting migrated Script
                if (includeProject) {
                    argument = "new " + argumentClass.getSimpleName() + "("+getProject()+", " + argument + ")";
                } else {
                    argument = "new " + argumentClass.getSimpleName() + "(" + argument + ")";
                }
            }

            generator.printString(taskName + "." + setterMethod.getName() + "(" + argument + ");");
        }
    }

    public String getProject() {
        if (this.contextName.equals("this")) {
            return "project";
        }
        else return this.contextName + ".project()";
    }

    private void generateProjectSetter(String taskName) {
        generator.printString(taskName + ".setProject("+getProject()+");");
    }

    public void generateAntCall(AntIntrospectionHelper introspectionHelper) {
        String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(introspectionHelper.getAttributeMap().get("target").toString()));
        //generator.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
        generator.printString("context = requireBuild(" + depName + "Builder.factory, context);");

        // TODO: Deal with children of antcall (params)
    }

    public void generateConstructor(AntIntrospectionHelper introspectionHelper, String taskName) {
        // Check for element macro constructor
        if (introspectionHelper.getParentIntrospectionHelper()!= null && introspectionHelper.getParentIntrospectionHelper().isMacroInvocationChildElement()) {
            TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();
            String parentName = introspectionHelper.getParentIntrospectionHelper().getName();
            String elementName = namingManager.getClassNameFor(introspectionHelper.getElement().getTaskName());

            generator.addImport(elementTypeClassName.getImportName());
            generator.printString(parentName+".configure"+elementName+"(new BiConsumer<"+elementTypeClassName.getShortName()+", "+getInputName()+">() {", "});");
            generator.increaseIndentation(1);

            generator.printString("@Override");
            generator.printString("public void execute("+elementTypeClassName.getShortName()+" "+taskName+", " + getInputName() + " ccontext) {", "}");
            generator.increaseIndentation(1);
        } else {
            TMethod constructorFactoryMethod = introspectionHelper.getConstructorFactoryMethod();
            TConstructor constructor = introspectionHelper.getConstructor();
            TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();

            if (constructorFactoryMethod != null) {
                // We have a factory method in the parent. Use it...
                generator.addImport(elementTypeClassName.getImportName());

                String taskClassName = elementTypeClassName.getShortName();

                constructedVariables.put(introspectionHelper.getElement(), new Pair<>(taskName, elementTypeClassName));

                if (!localScopedVariables)
                    generator.printString(taskName + " = " + introspectionHelper.getParentIntrospectionHelper().getName() + "." + constructorFactoryMethod.getName() + "();");
                else
                    generator.printString(taskClassName + " " + taskName + " = " + introspectionHelper.getParentIntrospectionHelper().getName() + "." + constructorFactoryMethod.getName() + "();");
            } else {
                if (constructor == null) {
                    throw new RuntimeException("We didn't have a constructor for " + taskName);
                } else {
                    ArrayList<String> params = new ArrayList<>();
                    // TODO: This is very fragile and possibly wrong
                    for (TParameter parameter : constructor.getParameters()) {
                        if (parameter.getTypeName().getFullyQualifiedName().equals("org.apache.tools.ant.Project"))
                            params.add(getProject());
                        else if (parameter.getName().equals("context"))
                            params.add("context");
                        else throw new RuntimeException("Encountered Constructor parameter that was not expected!");
                    }
                    generator.addImport(constructor.getDeclaringClassTypeName().getImportName());

                    constructedVariables.put(introspectionHelper.getElement(), new Pair<>(taskName, constructor.getDeclaringClassTypeName()));

                    if (!localScopedVariables)
                        generator.printString(taskName + " = new " + constructor.formatUse(params) + ";");
                    else
                        generator.printString(constructor.getDeclaringClassTypeName().getShortName() + " " + taskName + " = new " + constructor.formatUse(params) + ";");
                }
            }
        }
    }


}
