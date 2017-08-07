package antplutomigrator.generate.introspectionhelpers;

import antplutomigrator.generate.anthelpers.ReflectionHelpers;
import antplutomigrator.generate.types.TConstructor;
import antplutomigrator.generate.types.TMethod;
import antplutomigrator.generate.types.TTypeName;
import org.apache.tools.ant.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Created by manuel on 23.02.17.
 */
public class PlutoAntIntrospectionHelper extends AntIntrospectionHelper {

    private final IntrospectionHelper introspectionHelper;

    protected PlutoAntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, pkg, parentIntrospectionHelper);
        this.introspectionHelper = IntrospectionHelper.getHelper(getElementTypeClass());
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

    @Override
    public TConstructor getConstructor() {
        try {
            IntrospectionHelper.Creator creator = getElementCreator(getElement());
            Constructor<?> c = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
            TConstructor constructor = new TConstructor(c);
            if (constructor.getName().getFullyQualifiedName().equals("org.apache.tools.ant.taskdefs.Javac")) {
                constructor.setName(new TTypeName(this.getPkg() + ".NoIncrJavac"));
            }
            if (constructor.getDeclaringClassTypeName().getFullyQualifiedName().equals("org.apache.tools.ant.taskdefs.Javac")) {
                constructor.setDeclaringClassStringTypeName(new TTypeName(this.getPkg() + ".NoIncrJavac"));
            }
            return constructor;
        } catch (NullPointerException e) {

        }

        Constructor<?> constructor = null;
        try {
            // First try with Project.
            constructor = getElementTypeClass().getConstructor(Project.class);
        } catch (final NoSuchMethodException nme) {
            // OK, try without.
            try {
                constructor = getElementTypeClass().getConstructor();
            } catch (final NoSuchMethodException nme2) {
                // Well, no matching constructor.
            }
        }

        if (constructor == null)
            return null;

        TConstructor tConstructor = new TConstructor(constructor);

        if (tConstructor.getName().getFullyQualifiedName().equals("org.apache.tools.ant.taskdefs.Javac")) {
            tConstructor.setName(new TTypeName(this.getPkg() + ".NoIncrJavac"));
        }
        if (tConstructor.getDeclaringClassTypeName().getFullyQualifiedName().equals("org.apache.tools.ant.taskdefs.Javac")) {
            tConstructor.setDeclaringClassStringTypeName(new TTypeName(this.getPkg() + ".NoIncrJavac"));
        }

        return tConstructor;
    }

    public IntrospectionHelper.Creator getElementCreator(UnknownElement e) {
        assert (getElement() != null);
        try {
            final IntrospectionHelper.Creator elementCreator = introspectionHelper.getElementCreator(getProject(), "", null, e.getTaskName(), e);
            return elementCreator;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public boolean isProjectComponent() {
        /*boolean hasProjectSetter = false;
        for (Method method : getElementTypeClass().getMethods()) {
            if (method.getName().equals("setProject") && method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().equals("org.apache.tools.ant.Project")) {
                hasProjectSetter = true;
                break;
            }
        }
        return hasProjectSetter;*/
        return ProjectComponent.class.isAssignableFrom(getElementTypeClass());
    }

    @Override
    public Class<?> getElementTypeClass() {
        try {
            if (getParentIntrospectionHelper() != null && getParentIntrospectionHelper().getNestedElementType(getElement().getTaskName()) != null)
                return Class.forName(getParentIntrospectionHelper().getNestedElementType(getElement().getTaskName()).getFullyQualifiedName());
        } catch (Exception e) {
            // TODO: Deal with this more correctly
            //e.printStackTrace();
        }
        /*if (getParentIntrospectionHelper() != null && getParentIntrospectionHelper() instanceof  PlutoAntIntrospectionHelper) {
            // Try to find element class by looking into parent
            IntrospectionHelper.Creator creator = ((PlutoAntIntrospectionHelper) getParentIntrospectionHelper()).getElementCreator(getElement());
            Method creatorMethod = ReflectionHelpers.getNestedCreatorMethodFor(creator);
            if (creatorMethod.getAnnotatedReturnType().getType().getTypeName().equals("void") && creatorMethod.getParameterCount() == 1) {
                Constructor<?> constructor = ReflectionHelpers.getNestedCreatorConstructorFor(creator);
                elementTypeClass = ReflectionHelpers.getClassFor(constructor.getAnnotatedReturnType().getType().getTypeName());
            } else
                elementTypeClass = ReflectionHelpers.getClassFor(creatorMethod.getAnnotatedReturnType().getType().getTypeName());
            return elementTypeClass;
        }*/
        return super.getElementTypeClass();
    }

    @Override
    public TMethod getCreatorMethod(UnknownElement element) {
        IntrospectionHelper.Creator creator = this.getElementCreator(element);
        Method method = ReflectionHelpers.getNestedCreatorMethodFor(creator);
        if (method == null)
            return null;
        return new TMethod(method);
    }

    @Override
    public boolean hasImplicitElement() {
        return false;
    }

    @Override
    public String getImplicitElementName() {
        return null;
    }

    @Override
    public List<String> getSupportedNestedElements() {
        return Collections.list(introspectionHelper.getNestedElements());
    }

    @Override
    public TTypeName getNestedElementType(String name) {
        return new TTypeName(introspectionHelper.getElementType(name));
    }

    @Override
    public boolean isTask() {
        /*try {
            return this.getElementTypeClass().getMethod("init") != null;
        } catch (NoSuchMethodException e) {
            return false;
        }*/
        return Task.class.isAssignableFrom(getElementTypeClass());
    }

    @Override
    public boolean hasExecuteMethod() {
        try {
            return this.getElementTypeClass().getMethod("execute") != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        return new TMethod(introspectionHelper.getAttributeMethod(attr));
    }

    @Override
    public boolean supportsNestedElement(String name) {
        if (getMacroIntrospectionHelperThatSupportsElement(name) != null)
            return true;
        return introspectionHelper.supportsNestedElement(getElement().getNamespace(), name, getProject(), getParent());
    }
}
