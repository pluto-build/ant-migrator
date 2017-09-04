package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.types.TMethod;
import antplutomigrator.generate.types.TTypeName;
import antplutomigrator.utils.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.lang.reflect.Constructor;
import java.util.Map;

public class ConfigureTaskTransformer extends Transformer {
    public ConfigureTaskTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() {
        TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();
        if (elementTypeClassName == null)
            throw new RuntimeException("Could not get type definition for " + element.getTaskName());

        if (introspectionHelper.isTask()) {
            generator.printString(elementGenerator.getContextName() + ".initTask("+taskName+");");
        } else if (introspectionHelper.isProjectComponent())
            generator.printString(elementGenerator.getContextName() + ".initElement("+taskName+");");

        try {
            // Just for debugging purposes right now
            // TODO: Remove in release
            element.maybeConfigure();
        } catch (Throwable t) {

        }

        generateAttributes(element, taskName, introspectionHelper);

        generateText(element, taskName);

        generateChildren(element, taskName, introspectionHelper);

        generateMacroInvocationSpecificCode(introspectionHelper);

        if (introspectionHelper.hasExecuteMethod() && introspectionHelper.getParentIntrospectionHelper() == null)
            generator.printString(taskName + ".execute();");
    }

    public void generateAttributes(UnknownElement element, String taskName, AntIntrospectionHelper introspectionHelper) {
        for (Map.Entry<String, Object> entry : introspectionHelper.getAttributeMap().entrySet()) {
            String n = entry.getKey().toLowerCase();
            Object o = entry.getValue();

            if (n.equals("id")) {
                // We have a reference id. Add code to add it to the project.
                generator.printString(elementGenerator.getProject()+".addReference(\"" + element.getWrapper().getAttributeMap().get("id") + "\", " + taskName + ");");
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
                argument = elementGenerator.getContextName()+".resolveFile(\"" + escapedValue + "\")";
            } else if (EnumeratedAttribute.class.isAssignableFrom(argumentClass)) {
                TTypeName argumentClassName = new TTypeName(argumentClass.getName());
                String shortName = argumentClassName.getShortName();
                String attrName = namingManager.getNameFor(StringUtils.decapitalize(shortName));
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
                    argument = "new " + argumentClass.getSimpleName() + "("+elementGenerator.getProject()+", " + argument + ")";
                } else {
                    argument = "new " + argumentClass.getSimpleName() + "(" + argument + ")";
                }
            }

            generator.printString(taskName + "." + setterMethod.getName() + "(" + argument + ");");
        }
    }

    public void generateText(UnknownElement element, String taskName) {
        String text = element.getWrapper().getText().toString();
        if (!text.trim().isEmpty()) {
            text = resolver.getExpandedValue(StringEscapeUtils.escapeJava(text));
            generator.printString(taskName + ".addText(\"" + text + "\");");
        }
    }

    public void generateChildren(UnknownElement element, String taskName, AntIntrospectionHelper introspectionHelper) {
        if (element.getChildren() != null) {
            for (UnknownElement child : element.getChildren()) {
                generator.increaseIndentation(1);
                if (introspectionHelper.supportsNestedElement(child.getTaskName())) {
                    elementGenerator.generateElement(introspectionHelper, child, null, true);
                } else {
                    Class<?> elementTypeClass = introspectionHelper.getElementTypeClass();
                    if (elementTypeClass == null || !(TaskContainer.class.isAssignableFrom(elementTypeClass))) {
                        // Ignore macro child elements at definition...
                        if (!elementGenerator.getIgnoredMacroElements().contains(child.getTaskName()))
                            throw new RuntimeException("Didn't support nested element: " + child.getTaskName());
                    } else {
                        // a task container - anything could happen - just add the
                        // child to the container
                        String childName = elementGenerator.generateElement(introspectionHelper, child, null, true);
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

    public void generateMacroInvocationSpecificCode(AntIntrospectionHelper introspectionHelper) {
        // Close lambda
        if (introspectionHelper.getParentIntrospectionHelper()!= null && introspectionHelper.getParentIntrospectionHelper().isMacroInvocationChildElement()) {
            generator.closeOneLevel();
            generator.closeOneLevel();
        }
    }
}
