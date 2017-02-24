package generate;

import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TParameter;
import generate.types.TTypeName;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.EnumeratedAttribute;
import utils.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by manuel on 16.02.17.
 */
public class ElementGenerator {

    private final JavaGenerator generator;
    private final Project project;
    private final NamingManager namingManager;
    private final Resolvable resolver;

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public String getProjectName() {
        return getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Input";
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, Resolvable resolver) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
    }

    public String generateElement(AntIntrospectionHelper parentIntrospectionHelper, UnknownElement element) {

        String taskName = getNamingManager().getNameFor(StringUtils.decapitalize(element.getTaskName()));

        AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, taskName, generator.getPkg(), parentIntrospectionHelper);

        if (introspectionHelper.isAntCall()) {
            // Deal with antcalls

            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(introspectionHelper.getAttributeMap().get("target").toString()));
            //generator.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            generator.printString("cinput = requireBuild(" + depName + "Builder.factory, cinput.clone());");

            // TODO: Deal with children of antcall (params)

            return taskName;
        }

        Class<?> elementTypeClass = introspectionHelper.getElementTypeClass();
        if (elementTypeClass == null)
            throw new RuntimeException("Could not get type definition for " + element.getTaskName());

        TTypeName elementTypeClassName = new TTypeName(elementTypeClass.getName());

        TConstructor constructor = introspectionHelper.getConstructor();
        TMethod constructorFactoryMethod = introspectionHelper.getConstructorFactoryMethod();

        if (constructorFactoryMethod != null) {
            // We have a factory method in the parent. Use it...
            generator.addImport(elementTypeClassName.getImportName());

            String taskClassName = elementTypeClassName.getShortName();

            generator.printString(taskClassName + " " + taskName + " = " + introspectionHelper.getParentIntrospectionHelper().getName() + "." + constructorFactoryMethod.getName() + "();");
        } else {
            generateConstructor(introspectionHelper, constructor);
        }

        if (introspectionHelper.hasProjectSetter())
            generator.printString(taskName + ".setProject(project);");

        try {
            // Just for debugging purposes right now
            // TODO: Remove in release
            element.maybeConfigure();
        } catch (Throwable t) {

        }

        for (Map.Entry<String, Object> entry : introspectionHelper.getAttributeMap().entrySet()) {
            String n = entry.getKey().toLowerCase();
            Object o = entry.getValue();

            if (n.equals("id")) {
                // We have a reference id. Add code to add it to the project.
                generator.printString("project.addReference(\"" + element.getWrapper().getAttributeMap().get("id") + "\", " + taskName + ");");
                return taskName;
            }

            TMethod setterMethod = introspectionHelper.getAttributeMethod(n);

            // Get type of argument
            Class<?> argumentClass = introspectionHelper.getAttributeMethodType(n.toLowerCase());

            final String escapedValue = resolver.getExpandedValue(StringEscapeUtils.escapeJava(o.toString()));
            String argument = "\"" + escapedValue + "\"";
            if (argumentClass.getName().equals("boolean")) {
                // We expect a boolean, use true or false as values without wrapping into a string.
                argument = "Boolean.valueOf(\"" + escapedValue + "\")";
            } else if (java.io.File.class.equals(argumentClass)) {
                argument = "project.resolveFile(\"" + escapedValue + "\")";
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
                    argument = "new " + argumentClass.getSimpleName() + "(project, " + argument + ")";
                } else {
                    argument = "new " + argumentClass.getSimpleName() + "(" + argument + ")";
                }
            }

            generator.printString(taskName + "." + setterMethod.getName() + "(" + argument + ");");
        }

        // Element might include text. Call addText method...
        String text = resolver.getExpandedValue(StringEscapeUtils.escapeJava(element.getWrapper().getText().toString()));
        if (!text.trim().isEmpty()) {
            generator.printString(taskName + ".addText(\"" + text + "\");");
        }

        if (element.getChildren() != null) {
            for (UnknownElement child : element.getChildren()) {
                generator.increaseIndentation(1);
                if (introspectionHelper.supportsNestedElement(child.getTaskName())) {
                    generateElement(introspectionHelper, child);
                } else {
                    if (!(TaskContainer.class.isAssignableFrom(elementTypeClass))) {
                        throw new RuntimeException("Didn't support nested element: " + child.getTaskName());
                    } else {
                        // a task container - anything could happen - just add the
                        // child to the container
                        String childName = generateElement(introspectionHelper, child);
                        generator.printString(taskName + ".addTask(" + childName + ");");
                    }

                }
                generator.increaseIndentation(-1);
            }
        }

        if (introspectionHelper.getAddChildMethod() != null) {
            generator.printString(introspectionHelper.getParentIntrospectionHelper().getName() + "." + introspectionHelper.getAddChildMethod().getName() + "(" + taskName + ");");
        }

        return taskName;
    }

    private String getContructor(Class<?> cls) {
        boolean includeProject;
        Constructor<?> c;
        try {
            // First try with Project.
            c = cls.getConstructor(Project.class);
            includeProject = true;
        } catch (final NoSuchMethodException nme) {
            // OK, try without.
            try {
                c = cls.getConstructor();
                includeProject = false;
            } catch (final NoSuchMethodException nme2) {
                // Well, no matching constructor.
                throw new RuntimeException("We didn't find any matching constructor for type " + cls.toString());
            }
        }

        TTypeName clsName = new TTypeName(cls.getName());

        if (includeProject) {
            return "new " + clsName.getShortName() + "(project)";
        } else {
            return "new " + clsName.getShortName() + "()";
        }
    }

    private void generateConstructor(AntIntrospectionHelper introspectionHelper, TConstructor constructor) {
        TTypeName taskClassName = new TTypeName(introspectionHelper.getElementTypeClass().getName());

        if (constructor == null) {
            generator.addImport(taskClassName.getImportName());
            generator.printString(taskClassName.getShortName() + " " + introspectionHelper.getName() + " = " + getContructor(introspectionHelper.getElementTypeClass()) + ";");
        } else {
            ArrayList<String> params = new ArrayList<>();
            // TODO: This is very fragile and wrong
            for (TParameter parameter: constructor.getParameters()) {
                if (parameter.getName().equals("project"))
                    params.add("project");
                else if (parameter.getName().equals("input"))
                    params.add("cinput");
                else throw new RuntimeException("Encountered Constructor parameter that was not expected!");
            }
            generator.addImport(constructor.getDeclaringClassTypeName().getImportName());
            generator.printString(constructor.getDeclaringClassTypeName().getShortName() + " " + introspectionHelper.getName() + " = new " + constructor.formatUse(params) + ";");
        }
    }


}
