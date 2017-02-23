package generate;

import generate.types.TConstructor;
import generate.types.TMethod;
import generate.types.TParameter;
import generate.types.TTypeName;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.MacroDef;
import utils.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by manuel on 23.02.17.
 */
public class MacroAntIntrospectionHelper extends AntIntrospectionHelper {

    private MacroDef macroDef;
    private final NamingManager namingManager;

    protected MacroAntIntrospectionHelper(Project project, UnknownElement element, String name, AntIntrospectionHelper parentIntrospectionHelper) {
        super(project, element, name, parentIntrospectionHelper);
        namingManager = new NamingManager();
    }

    public MacroDef getMacroDef() {
        if (macroDef != null)
            return macroDef;

        try {
            Class<?> myAntTypeDefinitionClass = Class.forName("org.apache.tools.ant.taskdefs.MacroDef$MyAntTypeDefinition");
            Field field = myAntTypeDefinitionClass.getDeclaredField("macroDef");
            field.setAccessible(true);
            macroDef = (MacroDef) field.get(getAntTypeDefinition());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return macroDef;
    }

    @Override
    public TConstructor getConstructor() {
        ArrayList<TParameter> parameters = new ArrayList<>();
        parameters.add(new TParameter("project", new TTypeName(Project.class.getName())));
        parameters.add(new TParameter("input", new TTypeName(namingManager.getClassNameFor(getProject().getName() + "Input"))));
        return new TConstructor(namingManager.getClassNameFor(getElement().getTaskName()), parameters, new TTypeName(namingManager.getClassNameFor(getElement().getTaskName())));
    }

    @Override
    public boolean hasProjectSetter() {
        return false;
    }

    @Override
    public TMethod getAttributeMethod(String attr) {
        ArrayList<TParameter> parameters = new ArrayList<>();
        parameters.add(new TParameter(namingManager.getClassNameFor(attr), new TTypeName(String.class.getName())));
        return new TMethod("set" + namingManager.getClassNameFor(attr), parameters, new TTypeName(void.class.getName()));
    }

    @Override
    public Class<?> getAttributeMethodType(String attr) {
        return String.class;
    }

    @Override
    public boolean supportsNestedElement(String name) {
        // TODO:
        return false;
    }

    @Override
    public TMethod getConstructorFactoryMethod() {
        return null;
    }

    @Override
    public TMethod getAddChildMethod() {
        return null;
    }
}
