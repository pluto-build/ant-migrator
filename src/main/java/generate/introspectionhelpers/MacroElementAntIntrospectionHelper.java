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
    private final MacroAntIntrospectionHelper macroAntIntrospectionHelper;

    public NamingManager getNamingManager() {
        return namingManager;
    }

    protected MacroElementAntIntrospectionHelper(Project project, UnknownElement element, String name, String pkg, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, pkg, parentIntrospectionHelper);
        this.namingManager = new NamingManager();
        this.macroAntIntrospectionHelper = parentIntrospectionHelper.getMacroIntrospectionHelperThatSupportsElement(element.getTaskName());
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
        return getCommonNestedElements();
    }

    @Override
    public TTypeName getNestedElementType(String name) {
        List<AntIntrospectionHelper> parentAntIntroSpectionHelpers = getParentAntIntrospectionHelpers();
        assert(parentAntIntroSpectionHelpers.size() > 0);

        return parentAntIntroSpectionHelpers.get(0).getNestedElementType(name);
        // TODO
        //return null;
    }

    @Override
    public boolean isMacroInvocationChildElement() {
        return true;
    }

    public UnknownElement getMacroDefElement() {
        Target topTarget = getProject().getTargets().get("");
        UnknownElement macroDefElement = null;
        for (Task task: topTarget.getTasks()) {
            UnknownElement uTask = (UnknownElement)task;
            if (uTask.getTaskName().equals("macrodef") && uTask.getWrapper().getAttributeMap().get("name").equals(macroAntIntrospectionHelper.getElement().getTaskName())) {
                macroDefElement = uTask;
            }
        }
        return macroDefElement;
    }

    public UnknownElement getSequentialElement() {
        return getMacroDefElement().getChildren().stream().filter(element ->  element.getTaskName().equals("sequential")).findFirst().get();
    }

    public List<String> getCommonNestedElements(List<AntIntrospectionHelper> introspectionHelpers) {
        assert(introspectionHelpers != null && introspectionHelpers.size() > 1);
        List<AntIntrospectionHelper> helpers = new ArrayList<>();
        helpers.addAll(introspectionHelpers);
        List<String> commonSupportedNestedElements = helpers.get(0).getSupportedNestedElements();
        helpers.remove(0);
        for (AntIntrospectionHelper helper: helpers) {
            commonSupportedNestedElements.retainAll(helper.getSupportedNestedElements());
        }
        return commonSupportedNestedElements;
    }

    public List<String> getCommonNestedElements() {

        List<AntIntrospectionHelper> introspectionHelpers = getParentAntIntrospectionHelpers();

        return getCommonNestedElements(introspectionHelpers);
    }

    private List<AntIntrospectionHelper> getParentAntIntrospectionHelpers() {
        UnknownElement sequential = getSequentialElement();
        String macroElement = getElement().getTaskName();
        List<UnknownElement> parents = AntIntrospectionHelper.findParentsForNestedElement(sequential, macroElement);

        return parents.stream().map(parent -> {
            UnknownElement parentParent = AntIntrospectionHelper.findParentForNestedElement(this.getMacroDefElement(), parent);
            AntIntrospectionHelper parentIntrospectionHelper;
            if (parentParent.equals(this.getSequentialElement()))
                parentIntrospectionHelper = macroAntIntrospectionHelper;
            else
                parentIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(getProject(), parentParent, parentParent.getTaskName(),getPkg(), null);
            return AntIntrospectionHelper.getInstanceFor(getProject(), parent, parent.getTaskName(), getPkg().replace(".macros", ""), parentIntrospectionHelper);
        }).collect(Collectors.toList());
    }
}
