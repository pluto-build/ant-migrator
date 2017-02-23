package generate;

import com.sun.tools.javac.code.Symbol;
import generate.anthelpers.ReflectionHelpers;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.types.EnumeratedAttribute;
import utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    public void generateElement(String taskName, UnknownElement element, Class<?> elementTypeClass, boolean noConstructor) {
        AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, elementTypeClass);

        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);


        if (introspectionHelper.isAntCall()) {
            // Deal with antcalls

            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(introspectionHelper.getAttributeMap().get("target").toString()));
            //generator.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            generator.printString("cinput = requireBuild(" + depName + "Builder.factory, cinput.clone());");

            // TODO: Deal with children of antcall (params)

            return;
        }

        AntTypeDefinition typeDefinition = introspectionHelper.getAntTypeDefinition();
        try {
            if (elementTypeClass == null) {
                elementTypeClass = introspectionHelper.getElementTypeClass();
                if (elementTypeClass == null)
                    throw new RuntimeException("Could not get type definition for " + element.getTaskName());
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("Could not get type definition for " + element.getTaskName());
        }

        if (typeDefinition != null && typeDefinition.getClass().getSimpleName().equals("MyAntTypeDefinition")) {
            // We have a macro invocation. Deal with it differently

            MacroDef macroDef = null;
            try {
                Class<?> myAntTypeDefinitionClass = Class.forName("org.apache.tools.ant.taskdefs.MacroDef$MyAntTypeDefinition");
                Field field = myAntTypeDefinitionClass.getDeclaredField("macroDef");
                field.setAccessible(true);
                macroDef = (MacroDef) field.get(typeDefinition);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            generateMacroElement(taskName, element, elementTypeClass, noConstructor, macroDef);
            return;
        }

        Constructor<?> constructor = introspectionHelper.getConstructor();

        if (!noConstructor) {
            if (constructor == null) {
                generateConstructor(taskName, elementTypeClass);
            } else {
                // TODO: We have a contructor, use it and not the "general one"
                System.out.println("CONSTRUCTOR!: " + constructor.toGenericString());

                generateConstructor(taskName, elementTypeClass);
            }
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
                return;
            }

            String setter = introspectionHelper.getAttributeMethodName(n);

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
                String completeClassName = argumentClass.getCanonicalName();
                String shortName = argumentClass.getSimpleName();
                String attrName = getNamingManager().getNameFor(StringUtils.decapitalize(shortName));
                generator.addImport(completeClassName);
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

            generator.printString(taskName + "." + setter + "(" + argument + ");");
        }

        // Element might include text. Call addText method...
        String text = resolver.getExpandedValue(element.getWrapper().getText().toString());
        if (!text.trim().isEmpty()) {
            generator.printString(taskName + ".addText(\"" + StringEscapeUtils.escapeJava(text) + "\");");
        }

        if (element.getChildren() != null) {
            for (UnknownElement child : element.getChildren()) {
                generator.increaseIndentation(1);
                if (introspectionHelper.supportsNestedElement(child.getTaskName())) {
                    IntrospectionHelper.Creator ccreator = introspectionHelper.getIntrospectionHelper().getElementCreator(project, "", null, child.getTaskName(), child);
                    Method method = ReflectionHelpers.getNestedCreatorMethodFor(ccreator);
                    Constructor<?> cconstructor = ReflectionHelpers.getNestedCreatorConstructorFor(ccreator);
                    if (method == null)
                        throw new RuntimeException("Unexpected exception inspecting ant framework...");

                    String childName = getNamingManager().getNameFor(StringUtils.decapitalize(child.getTaskName()));

                    if (method.getAnnotatedReturnType().getType().getTypeName().equals("void")) {
                        if (cconstructor != null) {
                            Class<?> cls = null;
                            try {
                                cls = Class.forName(cconstructor.getAnnotatedReturnType().getType().getTypeName());
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            generateElement(childName, child, cls, false);
                        } else
                            generateElement(childName, child, null, false);
                        generator.printString(taskName + "." + method.getName() + "(" + childName + ");");
                    } else {
                        String fullyQualifiedChildTypeName = method.getReturnType().getCanonicalName();
                        String childTypeName = method.getReturnType().getSimpleName();

                        generator.addImport(fullyQualifiedChildTypeName);
                        generator.printString(childTypeName + " " + childName + " = " + taskName + "." + method.getName() + "();");

                        String returnTypeName = method.getReturnType().getName();

                        Class<?> cls = null;
                        try {
                            cls = Class.forName(returnTypeName);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                        generateElement(childName, child, cls, true);
                    }
                } else {
                    if (!(TaskContainer.class.isAssignableFrom(elementTypeClass))) {
                        throw new RuntimeException("Didn't support nested element: " + child.getTaskName());
                    } else {
                        // a task container - anything could happen - just add the
                        // child to the container

                        //TaskContainer container = (TaskContainer) parent;
                        //container.addTask(child);

                        String childName = getNamingManager().getNameFor(StringUtils.decapitalize(child.getTaskName()));

                        generateElement(childName, child, null, false);

                        generator.printString(taskName + ".addTask(" + childName + ");");
                    }

                }
                generator.increaseIndentation(-1);
            }
        }
    }

    private void generateMacroElement(String taskName, UnknownElement element, Class<?> elementTypeClass, boolean noConstructor, MacroDef macroDef) {
        String macroDefName = "";
        try {
            Field macroDefNameField = macroDef.getClass().getDeclaredField("name");
            macroDefNameField.setAccessible(true);
            macroDefName = (String) macroDefNameField.get(macroDef);
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve name of macro.");
        }

        String definedName = macroDefName + "Macro";
        String taskClassName = namingManager.getClassNameFor(definedName);

        // TODO: Generate everything here...

        // Constructor
        if (!noConstructor) {
            generator.addImport(generator.getPkg() + ".macros." + taskClassName);
            generator.printString(taskClassName + " " + taskName + " = new " + taskClassName + "(project, input);");
        }

        // text
        String text = resolver.getExpandedValue(element.getWrapper().getText().toString());
        if (!text.trim().isEmpty()) {
            generator.printString(taskName + ".addText(\"" + text.replace("\n", "\\n") + "\");");
        }

        // attributes
        for (Map.Entry<String, Object> entry : element.getWrapper().getAttributeMap().entrySet()) {
            String n = entry.getKey();
            String o = String.valueOf(entry.getValue());

            generator.printString(taskName + ".set" + namingManager.getClassNameFor(n) + "(\"" + StringEscapeUtils.escapeJava(o) + "\");");
        }
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

        if (includeProject) {
            return "new " + cls.getSimpleName() + "(project)";
        } else {
            return "new " + cls.getSimpleName() + "()";
        }
    }

    private void generateConstructor(String taskName, Class<?> elementTypeClass) {
        String fullyQualifiedTaskdefName = elementTypeClass.getCanonicalName();
        generator.addImport(fullyQualifiedTaskdefName);

        String taskClassName = elementTypeClass.getSimpleName();

        generator.printString(taskClassName + " " + taskName + " = " + getContructor(elementTypeClass) + ";");
    }


}
