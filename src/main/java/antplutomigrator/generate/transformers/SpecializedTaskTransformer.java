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
        return element.getWrapper().getAttributeMap().keySet().containsAll(Arrays.asList(attr)) &&
                Arrays.asList(attr).containsAll(element.getWrapper().getAttributeMap().keySet());
    }
}
