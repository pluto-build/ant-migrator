package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public class AvailableTransformer extends SpecializedConditionTransformer {
    public AvailableTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("file");
    }

    @Override
    public String transformCondition() {
        return generateToFile(attributeForKey("file"))+".exists()";
    }
}
