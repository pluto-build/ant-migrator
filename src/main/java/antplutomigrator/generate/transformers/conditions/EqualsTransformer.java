package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public class EqualsTransformer extends SpecializedConditionTransformer {
    public EqualsTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("arg1", "arg2");
    }

    @Override
    public String transformCondition() {
        return generateToString(attributeForKey("arg1")) + ".equals(" + generateToString(attributeForKey("arg2")) + ")";
    }
}
