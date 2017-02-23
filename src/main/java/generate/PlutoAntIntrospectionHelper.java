package generate;

import generate.anthelpers.ReflectionHelpers;
import org.apache.tools.ant.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
public class PlutoAntIntrospectionHelper extends AntIntrospectionHelper {

    private final IntrospectionHelper introspectionHelper;


    public IntrospectionHelper getIntrospectionHelper() {
        return introspectionHelper;
    }

    protected PlutoAntIntrospectionHelper(Project project, UnknownElement element) {
        super(project, element);
        this.setElementTypeClass(getElementTypeClass());
        this.introspectionHelper = IntrospectionHelper.getHelper(getElementTypeClass());
    }

    protected PlutoAntIntrospectionHelper(Project project, UnknownElement element, Class<?> elementTypeClass) {
        super(project, element, elementTypeClass);
        this.introspectionHelper = IntrospectionHelper.getHelper(getElementTypeClass());
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return getElement().getWrapper().getAttributeMap();
    }

    @Override
    public Constructor<?> getConstructor() {
        Constructor<?> constructor = null;
        try {
            IntrospectionHelper.Creator creator = introspectionHelper.getElementCreator(getProject(), "", null, getElement().getTaskName(), getElement());
            constructor = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
        } catch (NullPointerException e) {

        }
        return constructor;
    }

    public boolean hasProjectSetter() {
        boolean hasProjectSetter = false;
        for (Method method : getElementTypeClass().getMethods()) {
            if (method.getName().equals("setProject") && method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().equals("org.apache.tools.ant.Project")) {
                hasProjectSetter = true;
                break;
            }
        }
        return hasProjectSetter;
    }

    @Override
    public String getAttributeMethodName(String attr) {
        return introspectionHelper.getAttributeMethod(attr).getName();
    }

    @Override
    public Class<?> getAttributeMethodType(String attr) {
        return introspectionHelper.getAttributeType(attr);
    }

    @Override
    public boolean supportsNestedElement(String name) {
        return introspectionHelper.supportsNestedElement(getElement().getNamespace(), name);
    }
}
