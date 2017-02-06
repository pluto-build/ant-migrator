package utils;

import java.lang.reflect.Method;

/**
 * Created by manuel on 31.01.17.
 */
public class ReflectionUtils {

    public static Class<?> getTypeOfFirstArgument(Method m) {
        return m.getParameterTypes()[0];
    }

    public static Method getSetterForPorperty(Class<?> c, String name) {
        for (Method method: c.getMethods()) {
            if (method.getName().equalsIgnoreCase("set" + name))
                return method;
        }
        return null;
    }
}
