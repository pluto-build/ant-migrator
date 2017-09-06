package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

import java.util.Arrays;

public abstract class SpecializedTaskTransformer extends Transformer {
    public SpecializedTaskTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
        assert this.getClass().getSimpleName().equalsIgnoreCase(element.getTaskName() + "Transformer");
    }

    public boolean containsOnlySupportedAttributes(String... attr) {
        return Arrays.asList(attr).containsAll(element.getWrapper().getAttributeMap().keySet());
    }

    public String attributeForKey(String key) {
        Object attr = element.getWrapper().getAttributeMap().get(key);
        // TODO: Correct handling for null attributes
        if (attr == null)
            return null;
        return expand(attr.toString());
    }

    public String expand(String str) {
        return resolver.getExpandedValue(str);
    }
}
