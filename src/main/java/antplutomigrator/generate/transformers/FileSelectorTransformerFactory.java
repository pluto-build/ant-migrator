package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.utils.StringUtils;
import org.apache.tools.ant.UnknownElement;

import java.lang.reflect.InvocationTargetException;

public class FileSelectorTransformerFactory {
    public static FileSelectorTransformer getTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        try {
            Class<SpecializedFileSelectorTransformer> transformerCls = (Class<SpecializedFileSelectorTransformer>) Class.forName("antplutomigrator.generate.transformers.fileselectors." + StringUtils.capitalize(element.getTaskName().toLowerCase()) + "Transformer");
            SpecializedFileSelectorTransformer specializedTransformer = transformerCls.getConstructor(UnknownElement.class, ElementGenerator.class, AntIntrospectionHelper.class, String.class, String.class).newInstance(element, elementGenerator, introspectionHelper, baseDir, fileName);
            if (specializedTransformer.supportsElement())
                return specializedTransformer;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
        }
        return new DefaultFileSelectorTransformer(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }
}
