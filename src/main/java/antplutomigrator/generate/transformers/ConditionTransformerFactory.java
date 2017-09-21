package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.utils.StringUtils;
import org.apache.tools.ant.UnknownElement;

import java.lang.reflect.InvocationTargetException;

public class ConditionTransformerFactory {
    public static ConditionTransformer getTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        try {
            Class<SpecializedConditionTransformer> transformerCls = (Class<SpecializedConditionTransformer>) Class.forName("antplutomigrator.generate.transformers.conditions." + StringUtils.capitalize(element.getTaskName().toLowerCase()) + "Transformer");
            SpecializedConditionTransformer specializedTransformer = transformerCls.getConstructor(UnknownElement.class, ElementGenerator.class, AntIntrospectionHelper.class).newInstance(element, elementGenerator, introspectionHelper);
            if (specializedTransformer.supportsElement())
                return specializedTransformer;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
        }
        return new DefaultConditionTransformer(element, elementGenerator, introspectionHelper);
    }
}
