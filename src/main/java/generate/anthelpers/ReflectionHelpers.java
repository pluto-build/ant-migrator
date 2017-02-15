package generate.anthelpers;

import com.sun.tools.internal.jxc.ap.Const;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Target;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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
}
