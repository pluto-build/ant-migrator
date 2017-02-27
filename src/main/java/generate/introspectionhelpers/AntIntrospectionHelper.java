package generate.introspectionhelpers;

import generate.anthelpers.ReflectionHelpers;
import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TTypeName;
import org.apache.tools.ant.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
abstract public class AntIntrospectionHelper {

    private final Project project;
    private final String name;
    private final String pkg;
    private final UnknownElement element;
    private final AntIntrospectionHelper parentIntrospectionHelper;
    private final ComponentHelper componentHelper;
    protected Class<?> elementTypeClass;
    protected AntTypeDefinition antTypeDefinition;

    public AntIntrospectionHelper getParentIntrospectionHelper() {
        return parentIntrospectionHelper;
    }

    public PlutoAntIntrospectionHelper getParentPlutoAntIntrospectionHelper() {
        if (getParentIntrospectionHelper() == null)
            return null;
        if (getParentIntrospectionHelper() instanceof PlutoAntIntrospectionHelper)
            return (PlutoAntIntrospectionHelper) getParentIntrospectionHelper();
        return getParentIntrospectionHelper().getParentPlutoAntIntrospectionHelper();
    }

    public MacroAntIntrospectionHelper getParentMacroAntIntrospectionHelper() {
        if (getParentIntrospectionHelper() == null)
            return null;
        if (getParentIntrospectionHelper() instanceof MacroAntIntrospectionHelper)
            return (MacroAntIntrospectionHelper) getParentIntrospectionHelper();
        return getParentIntrospectionHelper().getParentMacroAntIntrospectionHelper();
    }

    public ComponentHelper getComponentHelper() {
        return componentHelper;
    }


    public String getName() {
        return name;
    }

    public String getPkg() {
        return pkg;
    }

    public Project getProject() {
        return project;
    }

    public UnknownElement getElement() {
        return element;
    }

    protected AntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        this.project = project;
        this.element = element;
        this.name = name;
        this.pkg = pkg;
        this.parentIntrospectionHelper = parentIntrospectionHelper;
        this.componentHelper = ComponentHelper.getComponentHelper(project);
    }

    public static AntIntrospectionHelper getInstanceFor(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);
        final AntTypeDefinition definition = componentHelper.getDefinition(element.getTaskName());
        if ((definition != null && definition.getClass().getSimpleName().equals("MyAntTypeDefinition"))) {
            return new MacroAntIntrospectionHelper(project, element, name, pkg, parentIntrospectionHelper);
        } else if (parentIntrospectionHelper != null && parentIntrospectionHelper.getFirstIntrospectionHelperThatSupportsElement(element.getTaskName()) instanceof MacroAntIntrospectionHelper) {
            return new MacroElementAntIntrospectionHelper(project, element, name, pkg, parentIntrospectionHelper);
        }
        return new PlutoAntIntrospectionHelper(project, element, name, pkg, parentIntrospectionHelper);
    }

    protected AntTypeDefinition getAntTypeDefinition() {
        if (antTypeDefinition != null)
            return antTypeDefinition;
        antTypeDefinition = getComponentHelper().getDefinition(element.getTaskName());
        return antTypeDefinition;
    }

    public Class<?> getElementTypeClass() {
        if (elementTypeClass != null)
            return elementTypeClass;

        final AntTypeDefinition antTypeDefinition = getAntTypeDefinition();
        if (antTypeDefinition == null) {
            return null;
        }
        elementTypeClass = antTypeDefinition.getTypeClass(project);

        return elementTypeClass;
    }

    public TTypeName getElementTypeClassName() {
        if (getElementTypeClass() == null)
            return null;
        return new TTypeName(getElementTypeClass().getName());
    }

    public boolean isMacroInvocation() {
        return getAntTypeDefinition() != null && getAntTypeDefinition().getClass().getSimpleName().equals("MyAntTypeDefinition");
    }

    public boolean isAntCall() {
        return element.getTaskName().equals("antcall");
    }

    public Map<String, Object> getAttributeMap() {
        if (getElement().getWrapper() == null)
            return new HashMap<>();
        return getElement().getWrapper().getAttributeMap();
    }

    public abstract TConstructor getConstructor();

    public abstract boolean hasProjectSetter();

    public abstract TMethod getAttributeMethod(String attr);

    public Class<?> getAttributeMethodType(String attr) {
        return getAttributeMethod(attr).getParameters().get(0).getType();
    }

    public TTypeName getAttributeMethodTypeName(String attr) {
        return getAttributeMethod(attr).getParameters().get(0).getTypeName();
    }

    public abstract boolean supportsNestedElement(String name);

    public abstract TMethod getCreatorMethod(UnknownElement element);

    public TMethod getConstructorFactoryMethod() {
        if (getParentIntrospectionHelper() == null)
            return null;
        TMethod nestedCreatorMethod = getParentIntrospectionHelper().getCreatorMethod(getElement());
        if (nestedCreatorMethod == null || nestedCreatorMethod.getReturnTypeName().getFullyQualifiedName().equals("void"))
            return null;
        return nestedCreatorMethod;
    }

    public TMethod getAddChildMethod() {
        if (getParentIntrospectionHelper() == null)
            return null;
        TMethod nestedCreatorMethod = getParentIntrospectionHelper().getCreatorMethod(getElement());
        if (nestedCreatorMethod == null || !nestedCreatorMethod.getReturnTypeName().getFullyQualifiedName().equals("void"))
            return null;
        return nestedCreatorMethod;
    }

    public UnknownElement getParent() {
        if (getParentIntrospectionHelper() != null)
            return getParentIntrospectionHelper().getElement();
        return null;
    }

    public MacroAntIntrospectionHelper getMacroIntrospectionHelperThatSupportsElement(String name) {
        MacroAntIntrospectionHelper parentMacroIntroSpectionHelper = getParentMacroAntIntrospectionHelper();
        while (parentMacroIntroSpectionHelper != null) {
            if (parentMacroIntroSpectionHelper.supportsNestedElement(name))
                return parentMacroIntroSpectionHelper;
            parentMacroIntroSpectionHelper = parentMacroIntroSpectionHelper.getParentMacroAntIntrospectionHelper();
        }
        return null;
    }

    public AntIntrospectionHelper getFirstIntrospectionHelperThatSupportsElement(String name) {
        if (this.supportsNestedElement(name))
            return this;
        AntIntrospectionHelper parentIntrospectionHelper = getParentIntrospectionHelper();
        if (parentIntrospectionHelper != null)
            return parentIntrospectionHelper.getFirstIntrospectionHelperThatSupportsElement(name);
        return null;
    }

    public abstract boolean hasImplicitElement();
    public abstract String getImplicitElementName();
}
