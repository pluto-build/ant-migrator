package generate;

import generate.anthelpers.ReflectionHelpers;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.EnumeratedAttribute;
import utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by manuel on 16.02.17.
 */
public class ElementGenerator {

    private final JavaGenerator generator;
    private final Project project;
    private final NamingManager namingManager;
    private final PropertyResolver resolver;

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public String getProjectName() {
        return getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public String getInputName() {
        return getProjectName() + "Input";
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, PropertyResolver resolver) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
    }

    public void generateElement(String taskName, UnknownElement element, Class<?> elementTypeClass, boolean noConstructor) {

        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);


        if (element.getTaskName().equals("antcall")) {
            // Deal with antcalls

            String depName = StringUtils.capitalize(getNamingManager().getClassNameFor(element.getWrapper().getAttributeMap().get("target").toString()));
            generator.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
            generator.printString("requireBuild(" + depName + "Builder.factory, " + StringUtils.decapitalize(depName) + "Input);");

            return;
        }

        try {
            if (elementTypeClass == null) {
                AntTypeDefinition typeDefinition = componentHelper.getDefinition(element.getTaskName());
                typeDefinition = componentHelper.getDefinition(element.getTaskName());
                elementTypeClass = typeDefinition.getTypeClass(project);
                if (elementTypeClass == null)
                    throw new RuntimeException("Could not get type definition for " + element.getTaskName());
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("Could not get type definition for " + element.getTaskName());
        }

        //String taskName = getNamingManager().getNameFor(StringUtils.decapitalize(element.getTaskName()));

        final IntrospectionHelper introspectionHelper = IntrospectionHelper.getHelper(elementTypeClass);

        Constructor<?> constructor = null;
        try {
            IntrospectionHelper.Creator creator = introspectionHelper.getElementCreator(project, "", null, element.getTaskName(), element);
            constructor = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
        } catch (NullPointerException e) {

        }
        if (constructor == null) {
            if (!noConstructor) {
                String fullyQualifiedTaskdefName = elementTypeClass.getCanonicalName();
                generator.addImport(fullyQualifiedTaskdefName);

                String taskClassName = fullyQualifiedTaskdefName.substring(fullyQualifiedTaskdefName.lastIndexOf(".") + 1);

                generator.printString(taskClassName + " " + taskName + " = new " + taskClassName + "();");
            }
        } else {
            // TODO: We have a contructor
            System.out.println("CONSTRUCTOR!: " + constructor.toGenericString());

            String fullyQualifiedTaskdefName = elementTypeClass.getCanonicalName();
            generator.addImport(fullyQualifiedTaskdefName);

            String taskClassName = fullyQualifiedTaskdefName.substring(fullyQualifiedTaskdefName.lastIndexOf(".") + 1);

            generator.printString(taskClassName + " " + taskName + " = new " + taskClassName + "();");
        }
        boolean hasProjectSetter = false;
        for (Method method: elementTypeClass.getMethods()) {
            if (method.getName().equals("setProject") && method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().equals("org.apache.tools.ant.Project")) {
                hasProjectSetter = true;
                break;
            }
        }
        if (hasProjectSetter)
            generator.printString(taskName + ".setProject(project);");

        if (element.getWrapper().getAttributeMap().contains("id")) {
            // We have a reference id. Add code to add it to the project.
            generator.printString("project.addReference(\""+element.getWrapper().getAttributeMap().get("id")+"\", " + taskName + ");");
        }

        try {
            element.maybeConfigure();
        }
        catch (Throwable t) {

        }

        element.getWrapper().getAttributeMap().forEach((n, o) ->
                {
                    Method attributeMethod = introspectionHelper.getAttributeMethod(n.toLowerCase());

                    String setter = attributeMethod.getName();

                    // Get type of argument
                    Class<?> argumentClass = introspectionHelper.getAttributeType(n.toLowerCase());

                    String argument = StringUtils.javaPrint(o.toString());
                    if (argumentClass.getName().equals("boolean")) {
                        // We expect a boolean, use true or false as values without wrapping into a string.
                        argument = "Boolean.valueOf(\"" + resolver.getExpandedValue(o.toString()) + "\")";
                    } else if (EnumeratedAttribute.class.isAssignableFrom(argumentClass)) {
                        String completeClassName = argumentClass.getCanonicalName();
                        String shortName = argumentClass.getSimpleName();
                        String attrName = getNamingManager().getNameFor(shortName);
                        generator.printString(completeClassName + " " + attrName + " = new " + completeClassName + "();");
                        generator.printString(attrName + ".setValue(\"" + o.toString() + "\");");
                    } else if (!(argumentClass.getName().equals("java.lang.String")|| argumentClass.getName().equals("java.lang.Object"))) {

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
                            argument = "new " + argumentClass.getSimpleName() + "(project, " + resolver.getExpandedValue(argument) + ")";
                        } else {
                            argument = "new " + argumentClass.getSimpleName() + "(" + resolver.getExpandedValue(argument) + ")";
                        }
                    }

                    generator.printString(taskName + "." + setter + "(" + resolver.getExpandedValue(argument) + ");");
                }
        );

        if (element.getChildren() != null) {
            for (UnknownElement child: element.getChildren()) {
                if (introspectionHelper.supportsNestedElement("", child.getTaskName())) {
                    IntrospectionHelper.Creator ccreator = introspectionHelper.getElementCreator(project, "", null, child.getTaskName(), child);
                    Method method = ReflectionHelpers.getNestedCreatorMethodFor(ccreator);
                    Constructor<?> cconstructor = ReflectionHelpers.getNestedCreatorConstructorFor(ccreator);
                    if (method != null) {
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
                            }
                            else
                                generateElement(childName, child, null, false);
                            generator.printString(taskName + "." + method.getName() + "(" + childName + ");");
                        } else {
                            generator.printString(method.getAnnotatedReturnType().getType().getTypeName().replace("$", ".") + " " + childName + " = " + taskName + "." + method.getName() + "();");

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
                        throw new RuntimeException("Unexpected exception inspecting ant framework...");
                    }
                } else {
                    throw new RuntimeException("Didn't support nested element: " + child.getTaskName());
                }
            }
        }
    }

}
