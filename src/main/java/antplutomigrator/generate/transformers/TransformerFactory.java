package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.utils.StringUtils;
import org.apache.tools.ant.UnknownElement;

import java.lang.reflect.InvocationTargetException;

public class TransformerFactory {
    public static Transformer getTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        try {
            Class<Transformer> transformerCls = (Class<Transformer>) Class.forName("antplutomigrator.generate.transformers.tasks." + StringUtils.capitalize(element.getTaskName().toLowerCase()) + "Transformer");
            Transformer specializedTransformer = transformerCls.getConstructor(UnknownElement.class, ElementGenerator.class, AntIntrospectionHelper.class).newInstance(element, elementGenerator, introspectionHelper);
            // TODO: Also rework macro code, so that the second check can be removed!
            if (specializedTransformer.supportsElement() && !elementGenerator.isInMacro() && introspectionHelper.getParentIntrospectionHelper() == null)
                return specializedTransformer;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
        }
        return new DefaultTaskTransformer(element, elementGenerator, introspectionHelper);
    }
}
