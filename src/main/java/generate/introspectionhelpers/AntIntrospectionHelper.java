package generate.introspectionhelpers;

import generate.MigrationException;
import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TTypeName;
import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
abstract public class AntIntrospectionHelper {

    private final Project project;
    private String name;
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

    public void setName(String name) {
        this.name = name;
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

    /**
     * Retrieves the AntTypeDefinition for the current element.
     * May return null if not retrievable
     * @return the AntTypeDefinition
     */
    protected AntTypeDefinition getAntTypeDefinition() {
        if (antTypeDefinition != null)
            return antTypeDefinition;
        antTypeDefinition = getComponentHelper().getDefinition(element.getTaskName());
        return antTypeDefinition;
    }

    /**
     * Get's the class corresponding to the current element.
     * @throws MigrationException when class not found
     * @return the class that the element corresponds to
     */
    public Class<?> getElementTypeClass() {
        if (elementTypeClass != null)
            return elementTypeClass;

        final AntTypeDefinition antTypeDefinition = getAntTypeDefinition();
        if (antTypeDefinition == null) {
            return null;
        }
        elementTypeClass = antTypeDefinition.getTypeClass(project);

        if (elementTypeClass == null)
            throw new MigrationException("Couldn't retrieve class for element <" + element.getTaskName() + ">");

        return elementTypeClass;
    }

    public TTypeName getElementTypeClassName() {
        return new TTypeName(getElementTypeClass().getName());
    }

    /**
     * Determines if the current element is a macro invocation.
     * @return
     */
    public boolean isMacroInvocation() {
        return getAntTypeDefinition() != null && getAntTypeDefinition().getClass().getSimpleName().equals("MyAntTypeDefinition");
    }

    public boolean isMacroInvocationChildElement() {
        return false;
    }

    /**
     * Determines if the current element is a antcall.
     * @return
     */
    public boolean isAntCall() {
        return element.getTaskName().equals("antcall");
    }

    /**
     * Returns a map with all attributes in the current element or an empty map if there are none or there is no wrapper...
     * @return
     */
    public Map<String, Object> getAttributeMap() {
        if (getElement().getWrapper() == null)
            return new HashMap<>();
        return getElement().getWrapper().getAttributeMap();
    }

    /**
     * Retrieves the constructor for the current element.
     * @return
     */
    public abstract TConstructor getConstructor();

    /**
     * Determines if the current element has a project setter method.
     * @return
     */
    public abstract boolean hasProjectSetter();

    /**
     * Retrieves the setter method for a given attribute
     * @param attr the name of the attribute for which the setter should be retrieved
     * @return a TMethod if there is a setter
     */
    public abstract TMethod getAttributeMethod(String attr);

    /**
     * Returns the class (type) which the setter expects as a parameter
     * @param attr the name of the attribute for which the type should be retrieved
     * @return
     */
    public Class<?> getAttributeMethodType(String attr) {
        return getAttributeMethod(attr).getParameters().get(0).getType();
    }

    /**
     * Returns the TTypeName which the setter expects as a parameter
     * @param attr the name of the attribute for which the type should be retrieved
     * @return
     */
    public TTypeName getAttributeMethodTypeName(String attr) {
        return getAttributeMethod(attr).getParameters().get(0).getTypeName();
    }

    /**
     * Queries if the current element supports a child with the given name
     * @param name the name of the child
     * @return true if the cild is supported
     */
    public abstract boolean supportsNestedElement(String name);

    /**
     * Returns a creator method for a given child element.
     * @return a TMethod for which is either:
     *    1) a method that returns void and adds the child to the current element (takes 1 parameter)
     *    2) a method that returns an instance of the child element and expects no parameters
     *    3) null if there is on creator method
     */
    public abstract TMethod getCreatorMethod(UnknownElement element);

    /**
     * Retrieves a constructor factory method for the current element based on {@link AntIntrospectionHelper#getCreatorMethod(UnknownElement)} of the parent.
     * @return a factory method that returns an instance for the current element or null if there is none
     */
    public TMethod getConstructorFactoryMethod() {
        if (getParentIntrospectionHelper() == null)
            return null;
        TMethod nestedCreatorMethod = getParentIntrospectionHelper().getCreatorMethod(getElement());
        if (nestedCreatorMethod == null || nestedCreatorMethod.getReturnTypeName().getFullyQualifiedName().equals("void"))
            return null;
        return nestedCreatorMethod;
    }

    /**
     * Retrieves a method to add the current element to its parent (based on {@link AntIntrospectionHelper#getCreatorMethod(UnknownElement)}).
     * @return A method if the child can be added or null if there is none
     */
    public TMethod getAddChildMethod() {
        if (getParentIntrospectionHelper() == null)
            return null;
        TMethod nestedCreatorMethod = getParentIntrospectionHelper().getCreatorMethod(getElement());
        if (nestedCreatorMethod == null || !nestedCreatorMethod.getReturnTypeName().getFullyQualifiedName().equals("void"))
            return null;
        return nestedCreatorMethod;
    }

    /**
     * Retrieves the parent element
     * @return the parent or null if there is none
     */
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

    /**
     *
     * @return
     */
    public abstract boolean hasImplicitElement();

    public abstract String getImplicitElementName();

    public abstract List<String> getSupportedNestedElements();

    public abstract TTypeName getNestedElementType(String name);


    public List<UnknownElement> findParentsForNestedElement(String name) {
        return AntIntrospectionHelper.findParentsForNestedElement(getElement(), name);
    }

    public static List<UnknownElement> findParentsForNestedElement(UnknownElement element,  String name) {
        List<UnknownElement> parents = new ArrayList<>();
        if (element.getChildren() == null)
            return parents;
        if (element.getChildren().stream().anyMatch(c -> c.getTaskName().equals(name))) {
            parents.add(element);
        }
        for (UnknownElement c: element.getChildren()) {
            List<UnknownElement> res = findParentsForNestedElement(c, name);
            parents.addAll(res);
        }
        return parents;
    }

    public static UnknownElement findParentForNestedElement(UnknownElement root,  UnknownElement element) {
        if (root.getChildren() == null)
            return null;
        if (root.getChildren().stream().anyMatch(c -> c.equals(element))) {
            return root;
        }
        for (UnknownElement c: root.getChildren()) {
            UnknownElement res = findParentForNestedElement(c, element);
            if (res != null)
                return res;
        }
        return null;
    }
}
