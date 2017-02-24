package generate;

import generate.types.TConstructor;
import generate.types.TMethod;
import org.apache.tools.ant.*;

import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
abstract class AntIntrospectionHelper {

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


    public void setElementTypeClass(Class<?> elementTypeClass) {
        this.elementTypeClass = elementTypeClass;
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
        if (definition != null && definition.getClass().getSimpleName().equals("MyAntTypeDefinition")) {
            return new MacroAntIntrospectionHelper(project, element, name, pkg, parentIntrospectionHelper);
        } else {
            return new PlutoAntIntrospectionHelper(project, element, name, pkg, parentIntrospectionHelper);
        }
    }

    public AntTypeDefinition getAntTypeDefinition() {
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

    public boolean isMacroInvocation() {
        return getAntTypeDefinition() != null && getAntTypeDefinition().getClass().getSimpleName().equals("MyAntTypeDefinition");
    }

    public boolean isAntCall() {
        return element.getTaskName().equals("antcall");
    }

    public Map<String, Object> getAttributeMap() {
        return getElement().getWrapper().getAttributeMap();
    }

    public abstract TConstructor getConstructor();

    public abstract boolean hasProjectSetter();

    public abstract TMethod getAttributeMethod(String attr);

    public Class<?> getAttributeMethodType(String attr) {
        return getAttributeMethod(attr).getParameters().get(0).getType();
    }

    public abstract boolean supportsNestedElement(String name);

    public abstract TMethod getConstructorFactoryMethod();

    public abstract TMethod getAddChildMethod();
}
