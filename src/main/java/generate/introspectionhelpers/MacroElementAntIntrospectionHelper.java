package generate.introspectionhelpers;

import generate.NamingManager;
import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TTypeName;
import org.apache.tools.ant.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by manuel on 27.02.17.
 */
public class MacroElementAntIntrospectionHelper extends AntIntrospectionHelper {

    private final NamingManager namingManager;

    public NamingManager getNamingManager() {
        return namingManager;
    }

    protected MacroElementAntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, pkg, parentIntrospectionHelper);
        this.namingManager = new NamingManager();
    }

    @Override
    public TTypeName getElementTypeClassName() {
        return getParentIntrospectionHelper().getFirstIntrospectionHelperThatSupportsElement(getElement().getTaskName()).getNestedElementType(getElement().getTaskName());
    }

    @Override
    public TConstructor getConstructor() {
        // TODO
        return null;
    }

    @Override
    public boolean hasProjectSetter() {
        return false;
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        // TODO
        return null;
        //return getRealElementIntrospectionHelper().getAttributeMethod(attr);
    }

    @Override
    public boolean supportsNestedElement(String name) {
        // TODO
        return getSupportedNestedElements().contains(name);
        //return getRealElementIntrospectionHelper().supportsNestedElement(name);
    }

    @Override
    public TMethod getCreatorMethod(UnknownElement element) {
        // TODO
        return null;
        //return getRealElementIntrospectionHelper().getCreatorMethod(element);
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
        // TODO
        return getMacroAntIntrospectionHelper().getCommonNestedElements(getElement());
    }


    @Override
    public TTypeName getNestedElementType(String name) {
        List<AntIntrospectionHelper> parentAntIntroSpectionHelpers = getMacroAntIntrospectionHelper().getParentAntIntrospectionHelpers(getElement().getTaskName());
        assert(parentAntIntroSpectionHelpers.size() > 0);

        return parentAntIntroSpectionHelpers.get(0).getNestedElementType(name);
        // TODO
        //return null;
    }

    @Override
    public boolean isMacroInvocationChildElement() {
        return true;
    }

    public MacroAntIntrospectionHelper getMacroAntIntrospectionHelper() {
        return getParentIntrospectionHelper().getMacroIntrospectionHelperThatSupportsElement(getElement().getTaskName());
    }
}
