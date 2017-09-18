package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.types.TConstructor;
import antplutomigrator.generate.types.TMethod;
import antplutomigrator.generate.types.TParameter;
import antplutomigrator.generate.types.TTypeName;
import javafx.util.Pair;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

import java.util.ArrayList;

public class ConstructorTaskTransformer extends Transformer {
    public ConstructorTaskTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() throws RuntimeException {
        if (elementGenerator.isNoConstructor())
            return;

        // Check for element macro constructor
        if (introspectionHelper.getParentIntrospectionHelper()!= null && introspectionHelper.getParentIntrospectionHelper().isMacroInvocationChildElement()) {
            TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();
            String parentName = introspectionHelper.getParentIntrospectionHelper().getName();
            String elementName = namingManager.getClassNameFor(introspectionHelper.getElement().getTaskName());

            generator.addImport(elementTypeClassName.getImportName());
            generator.printString(parentName+".configure"+elementName+"(new BiConsumer<"+elementTypeClassName.getShortName()+", "+elementGenerator.getInputName()+">() {", "});");
            generator.increaseIndentation(1);

            generator.printString("@Override");
            generator.printString("public void execute("+elementTypeClassName.getShortName()+" "+taskName+", " + elementGenerator.getInputName() + " context) {", "}");
            generator.increaseIndentation(1);
        } else {
            TMethod constructorFactoryMethod = introspectionHelper.getConstructorFactoryMethod();
            TConstructor constructor = introspectionHelper.getConstructor();
            TTypeName elementTypeClassName = introspectionHelper.getElementTypeClassName();

            if (constructorFactoryMethod != null) {
                // We have a factory method in the parent. Use it...
                generator.addImport(elementTypeClassName.getImportName());

                String taskClassName = elementTypeClassName.getShortName();

                elementGenerator.getConstructedVariables().put(introspectionHelper.getElement(), new Pair<>(taskName, elementTypeClassName));

                if (!elementGenerator.isLocalScopedVariables())
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
                            params.add(elementGenerator.getProject());
                        else if (parameter.getTypeName().getFullyQualifiedName().equals("AntBuilder"))
                            if (elementGenerator.isInMacro())
                                params.add("builder");
                            else
                                params.add("this");
                        else if (parameter.getName().equals("context"))
                            params.add("context");
                        else throw new RuntimeException("Encountered Constructor parameter that was not expected!");
                    }
                    generator.addImport(constructor.getDeclaringClassTypeName().getImportName());

                    elementGenerator.getConstructedVariables().put(introspectionHelper.getElement(), new Pair<>(taskName, constructor.getDeclaringClassTypeName()));

                    if (!elementGenerator.isLocalScopedVariables())
                        generator.printString(taskName + " = new " + constructor.formatUse(params) + ";");
                    else
                        generator.printString(constructor.getDeclaringClassTypeName().getShortName() + " " + taskName + " = new " + constructor.formatUse(params) + ";");
                }
            }
        }

        // TODO: This is hacky
        if (elementGenerator.isOnlyConstructors() && element.getChildren() != null) {
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
    }
}
