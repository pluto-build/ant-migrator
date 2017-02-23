package generate;

import org.apache.tools.ant.*;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
abstract class AntIntrospectionHelper {

    private final Project project;
    private final UnknownElement element;
    private final ComponentHelper componentHelper;
    private Class<?> elementTypeClass;
    private AntTypeDefinition antTypeDefinition;

    public ComponentHelper getComponentHelper() {
        return componentHelper;
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

    protected AntIntrospectionHelper(Project project, UnknownElement element) {
        this.project = project;
        this.element = element;
        this.componentHelper = ComponentHelper.getComponentHelper(project);
    }

    protected AntIntrospectionHelper(Project project, UnknownElement element, Class<?> elementTypeClass) {
        this.project = project;
        this.element = element;
        this.componentHelper = ComponentHelper.getComponentHelper(project);
        this.elementTypeClass = elementTypeClass;
    }

    public static AntIntrospectionHelper getInstanceFor(Project project, UnknownElement element) {
        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);
        final AntTypeDefinition definition = componentHelper.getDefinition(element.getTaskName());
        if (definition != null && definition.getClass().getSimpleName().equals("MyAntTypeDefinition")) {
            return new MacroAntIntrospectionHelper(project, element);
        } else {
            return new PlutoAntIntrospectionHelper(project, element);
        }
    }

    public static AntIntrospectionHelper getInstanceFor(Project project, UnknownElement element, Class<?> elementTypeClass) {
        ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);
        final AntTypeDefinition definition = componentHelper.getDefinition(element.getTaskName());
        if (definition != null && definition.getClass().getSimpleName().equals("MyAntTypeDefinition")) {
            return new MacroAntIntrospectionHelper(project, element);
        } else {
            return new PlutoAntIntrospectionHelper(project, element, elementTypeClass);
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
        if (antTypeDefinition == null)
            return null;
        elementTypeClass = antTypeDefinition.getTypeClass(project);

        return elementTypeClass;
    }

    public boolean isMacroInvocation() {
        return getAntTypeDefinition() != null && getAntTypeDefinition().getClass().getSimpleName().equals("MyAntTypeDefinition");
    }

    public boolean isAntCall() {
        return element.getTaskName().equals("antcall");
    }

    public abstract Map<String, Object> getAttributeMap();

    public abstract Constructor<?> getConstructor();

    public abstract boolean hasProjectSetter();

    public abstract String getAttributeMethodName(String attr);

    public abstract Class<?> getAttributeMethodType(String attr);

    public abstract boolean supportsNestedElement(String name);

    public abstract IntrospectionHelper getIntrospectionHelper();
}
