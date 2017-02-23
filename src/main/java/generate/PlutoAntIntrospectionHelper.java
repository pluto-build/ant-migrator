package generate;

import generate.anthelpers.ReflectionHelpers;
import generate.types.TConstructor;
import generate.types.TMethod;
import org.apache.tools.ant.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by manuel on 23.02.17.
 */
public class PlutoAntIntrospectionHelper extends AntIntrospectionHelper {

    private final IntrospectionHelper introspectionHelper;

    protected PlutoAntIntrospectionHelper(Project project, UnknownElement element, String name, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, parentIntrospectionHelper);
        this.introspectionHelper = IntrospectionHelper.getHelper(getElementTypeClass());
    }

    @Override
    public TConstructor getConstructor() {
        try {
            IntrospectionHelper.Creator creator = getElementCreator(getElement());
            Constructor<?> c = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
            return new TConstructor(c);
        } catch (NullPointerException e) {

        }
        return null;
    }

    public IntrospectionHelper.Creator getElementCreator(UnknownElement e) {
        assert (getElement() != null);
        final IntrospectionHelper.Creator elementCreator = introspectionHelper.getElementCreator(getProject(), "", null, e.getTaskName(), e);
        return elementCreator;
    }

    @Override
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
    public Class<?> getElementTypeClass() {
        if (getParentIntrospectionHelper() != null) {
            // Try to find element class by looking into parent
            IntrospectionHelper.Creator creator = ((PlutoAntIntrospectionHelper) getParentIntrospectionHelper()).getElementCreator(getElement());
            Method creatorMethod = ReflectionHelpers.getNestedCreatorMethodFor(creator);
            if (creatorMethod.getAnnotatedReturnType().getType().getTypeName().equals("void") && creatorMethod.getParameterCount() == 1) {
                Constructor<?> constructor = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
                elementTypeClass = ReflectionHelpers.getClassFor(constructor.getAnnotatedReturnType().getType().getTypeName());
            } else
                elementTypeClass = ReflectionHelpers.getClassFor(creatorMethod.getAnnotatedReturnType().getType().getTypeName());
            return elementTypeClass;
        }
        return super.getElementTypeClass();
    }

    private Method getNestedCreatorMethod() {
        if (getParentIntrospectionHelper() != null) {
            IntrospectionHelper.Creator creator = ((PlutoAntIntrospectionHelper) getParentIntrospectionHelper()).getElementCreator(getElement());
            return ReflectionHelpers.getNestedCreatorMethodFor(creator);
        }
        return null;
    }

    @Override
    public TMethod getConstructorFactoryMethod() {
        Method nestedCreatorMethod = getNestedCreatorMethod();
        if (nestedCreatorMethod == null)
            return null;
        return new TMethod(nestedCreatorMethod);
    }

    @Override
    public TMethod getAddChildMethod() {
        Method nestedCreatorMethod = getNestedCreatorMethod();
        if (nestedCreatorMethod == null || !nestedCreatorMethod.getAnnotatedReturnType().getType().getTypeName().equals("void"))
            return null;
        return new TMethod(nestedCreatorMethod);
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        return new TMethod(introspectionHelper.getAttributeMethod(attr));
    }

    @Override
    public boolean supportsNestedElement(String name) {
        return introspectionHelper.supportsNestedElement(getElement().getNamespace(), name);
    }
}
