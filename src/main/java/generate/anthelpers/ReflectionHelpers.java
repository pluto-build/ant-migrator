package generate.anthelpers;

import com.sun.tools.internal.jxc.ap.Const;
import org.apache.commons.lang.ClassUtils;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.UnknownElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by manuel on 15.02.17.
 */
public class ReflectionHelpers {
    public static List<Object> getChildrenFor(Target target) {
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

    public static Method getNestedCreatorMethodFor(IntrospectionHelper.Creator creator) {
        try {
            Field field = IntrospectionHelper.Creator.class.getDeclaredField("nestedCreator");
            field.setAccessible(true);
            Object nestedCreator = field.get(creator);

            Class<?> nestedCreatorClass = Class.forName("org.apache.tools.ant.IntrospectionHelper$NestedCreator");

            Field methodField = nestedCreatorClass.getDeclaredField("method");
            methodField.setAccessible(true);
            Method method = (Method)methodField.get(nestedCreator);
            return method;
        } catch (NoSuchFieldException e) {
            //e.printStackTrace();
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        }
        return null;
    }

    public static Constructor<?> getNestedCreatorConstructorFor(IntrospectionHelper.Creator creator) {
        try {
            Field field = IntrospectionHelper.Creator.class.getDeclaredField("nestedCreator");
            field.setAccessible(true);
            Object nestedCreator = field.get(creator);

            Class<?> nestedCreatorClass = Class.forName("org.apache.tools.ant.IntrospectionHelper$AddNestedCreator");

            Field methodField = nestedCreatorClass.getDeclaredField("constructor");
            methodField.setAccessible(true);
            Constructor<?> constructor = (Constructor<?>) methodField.get(nestedCreator);
            return constructor;
        } catch (NoSuchFieldException e) {
            //e.printStackTrace();
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
        return null;
    }

    private static final Map<String, Class<?>> PRIMITIVE_TYPE_MAP = new HashMap<String, Class<?>>(8);
    static {

        final Class<?>[] primitives = {Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE,
                Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE};
        for (int i = 0; i < primitives.length; i++) {
            PRIMITIVE_TYPE_MAP.put (primitives[i].getName(), primitives[i]);
        }
    }

    public static Class<?> getClassFor(String name) {
        try {
            if (PRIMITIVE_TYPE_MAP.containsKey(name))
                return PRIMITIVE_TYPE_MAP.get(name);
            return Class.forName(name);
        } catch (ClassNotFoundException e) {

        }
        return null;
    }

    public static void clearChildrenFor(UnknownElement element) {
        try {
            Field field = UnknownElement.class.getDeclaredField("children");
            field.setAccessible(true);
            field.set(element, null);
        } catch (NoSuchFieldException e) {
            //e.printStackTrace();
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
        }
    }
}
