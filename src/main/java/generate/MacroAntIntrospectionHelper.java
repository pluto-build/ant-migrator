package generate;

import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.MacroDef;
import utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by manuel on 23.02.17.
 */
public class MacroAntIntrospectionHelper extends AntIntrospectionHelper {

    private MacroDef macroDef;
    private final NamingManager namingManager;

    protected MacroAntIntrospectionHelper(Project project, UnknownElement element) {
        super(project, element);
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
    public Map<String, Object> getAttributeMap() {
        return null;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public boolean hasProjectSetter() {
        return false;
    }

    @Override
    public String getAttributeMethodName(String attr) {
        return "set" + namingManager.getClassNameFor(attr);
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
    public IntrospectionHelper getIntrospectionHelper() {
        return null;
    }
}
