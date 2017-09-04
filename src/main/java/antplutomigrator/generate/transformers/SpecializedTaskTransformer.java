package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public abstract class SpecializedTaskTransformer extends Transformer {
    public SpecializedTaskTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
        assert this.getClass().getSimpleName().equalsIgnoreCase(element.getTaskName() + "Transformer");
    }
}
