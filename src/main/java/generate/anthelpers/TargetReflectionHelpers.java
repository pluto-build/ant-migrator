package generate.anthelpers;

import org.apache.tools.ant.Target;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by manuel on 09.02.17.
 */
public class TargetReflectionHelpers {
    public static List<Object> getChildren(Target target) {
        try {
            Field field = Target.class.getDeclaredField("children");
            field.setAccessible(true);
            List<Object> children = (List<Object>)field.get(target);
            return children;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
