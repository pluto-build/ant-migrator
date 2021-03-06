package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public abstract class ConditionTransformer extends Transformer {
    public ConditionTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    public abstract String transformCondition();
}
