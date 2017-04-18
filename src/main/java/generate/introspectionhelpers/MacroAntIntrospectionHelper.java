package generate.introspectionhelpers;

import generate.NamingManager;
import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TParameter;
import generate.types.TTypeName;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.MacroDef;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by manuel on 23.02.17.
 */
public class MacroAntIntrospectionHelper extends AntIntrospectionHelper {

    private MacroDef macroDef;
    private final NamingManager namingManager;

    protected MacroAntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, pkg, parentIntrospectionHelper);
        namingManager = new NamingManager();
    }

    public MacroDef getMacroDef() {
        if (macroDef != null)
            return macroDef;

        try {
            Class<?> myAntTypeDefinitionClass = Class.forName("org.apache.tools.ant.taskdefs.MacroDef$MyAntTypeDefinition");
            Field field = myAntTypeDefinitionClass.getDeclaredField("macroDef");
            field.setAccessible(true);
            macroDef = (MacroDef) field.get(getAntTypeDefinition());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return macroDef;
    }

    @Override
    public TConstructor getConstructor() {
        ArrayList<TParameter> parameters = new ArrayList<>();
        parameters.add(new TParameter("project", new TTypeName(Project.class.getName())));
        parameters.add(new TParameter("input", new TTypeName(namingManager.getClassNameFor(getProject().getName() + "Input"))));
        String pkg = getPkg();
        if (!pkg.endsWith(".macros"))
            pkg = pkg + ".macros";
        return new TConstructor(new TTypeName(namingManager.getClassNameFor(getElement().getTaskName() + "Macro")), parameters, new TTypeName(pkg + "." + namingManager.getClassNameFor(getElement().getTaskName() + "Macro")));
    }

    @Override
    public boolean hasProjectSetter() {
        return false;
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        ArrayList<TParameter> parameters = new ArrayList<>();
        parameters.add(new TParameter(namingManager.getClassNameFor(attr), new TTypeName(String.class.getName())));
        return new TMethod("set" + namingManager.getClassNameFor(attr), parameters, new TTypeName(void.class.getName()));
    }

    @Override
    public Class<?> getAttributeMethodType(String attr) {
        return String.class;
    }

    @Override
    public boolean hasImplicitElement() {
        return getMacroDef().getElements().values().stream().anyMatch(templateElement -> templateElement.isImplicit());
    }

    @Override
    public String getImplicitElementName() {
        return getMacroDef().getElements().values().stream().filter(templateElement -> templateElement.isImplicit()).findFirst().get().getName();
    }

    @Override
    public List<String> getSupportedNestedElements() {
        ArrayList<String> res = new ArrayList<>();
        res.addAll(getMacroDef().getElements().keySet());
        return res;
    }

    @Override
    public TTypeName getNestedElementType(String name) {
        if (!supportsNestedElement(name))
            return null;
        return getElementTypeClassName().appendNested(namingManager.getClassNameFor(name));
    }

    @Override
    public boolean supportsNestedElement(String name) {
        // TODO Implicit element...
        return getSupportedNestedElements().contains(name);// || hasImplicitElement();
    }

    @Override
    public TTypeName getElementTypeClassName() {
        if (!getPkg().endsWith(".macros"))
            return new TTypeName(getPkg() + ".macros." + namingManager.getClassNameFor(getElement().getTaskName()) + "Macro");
        else
            return new TTypeName(getPkg() + "." + namingManager.getClassNameFor(getElement().getTaskName()) + "Macro");
    }

    @Override
    public TMethod getCreatorMethod(UnknownElement element) {
        assert(element != null);
        if (!this.supportsNestedElement(element.getTaskName()))
            return null;

        MacroAntIntrospectionHelper macroAntIntrospectionHelper = getMacroIntrospectionHelperThatSupportsElement(element.getTaskName());
        if (macroAntIntrospectionHelper != null) {

            String getterName = "get" + namingManager.getClassNameFor(element.getTaskName());

            return new TMethod(getterName, new ArrayList<>(), macroAntIntrospectionHelper.getElementTypeClassName().appendNested(namingManager.getClassNameFor(element.getTaskName())));
        }
        return null;
    }

    @Override
    public TMethod getConstructorFactoryMethod() {
        return null;
    }

    @Override
    public TMethod getAddChildMethod() {
        return null;
    }

    @Override
    public MacroAntIntrospectionHelper getMacroIntrospectionHelperThatSupportsElement(String name) {
        if (this.supportsNestedElement(name))
            return this;
        return super.getMacroIntrospectionHelperThatSupportsElement(name);
    }
}
