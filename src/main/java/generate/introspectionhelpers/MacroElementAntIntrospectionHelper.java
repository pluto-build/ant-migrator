package generate.introspectionhelpers;

import generate.NamingManager;
import generate.types.TConstructor;
import generate.types.TMethod;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;

import java.util.ArrayList;

/**
 * Created by manuel on 27.02.17.
 */
public class MacroElementAntIntrospectionHelper extends AntIntrospectionHelper {

    private final NamingManager namingManager;
    private AntIntrospectionHelper realElementIntrospectionHelper = null;

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public AntIntrospectionHelper getRealElementIntrospectionHelper() {
        if (realElementIntrospectionHelper == null)
        {
            MacroAntIntrospectionHelper macroAntIntrospectionHelper = getMacroIntrospectionHelperThatSupportsElement(getElement().getTaskName());
            if (macroAntIntrospectionHelper != null) {
                UnknownElement parentElement = macroAntIntrospectionHelper.findParentForElement(macroAntIntrospectionHelper.getMacroDef().getNestedTask(), getElement().getTaskName());
                this.realElementIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(getProject(), parentElement, getName(), getPkg(), null);
            }
        }
        return realElementIntrospectionHelper;
    }

    protected MacroElementAntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, pkg, parentIntrospectionHelper);
        this.namingManager = new NamingManager();
    }

    @Override
    public Class<?> getElementTypeClass() {
        MacroAntIntrospectionHelper macroAntIntrospectionHelper = getMacroIntrospectionHelperThatSupportsElement(getElement().getTaskName());
        if (macroAntIntrospectionHelper != null) {
            return getRealElementIntrospectionHelper().getElementTypeClass();
        }
        return null;
    }

    @Override
    public TConstructor getConstructor() {
        return getRealElementIntrospectionHelper().getConstructor();
    }

    @Override
    public boolean hasProjectSetter() {
        return getRealElementIntrospectionHelper().hasProjectSetter();
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        return getRealElementIntrospectionHelper().getAttributeMethod(attr);
    }

    @Override
    public boolean supportsNestedElement(String name) {
        return getRealElementIntrospectionHelper().supportsNestedElement(name);
    }

    @Override
    public TMethod getCreatorMethod(UnknownElement element) {
        return getRealElementIntrospectionHelper().getCreatorMethod(element);
    }


}
