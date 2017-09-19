package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public class IssetTransformer extends SpecializedConditionTransformer {
    public IssetTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("property");
    }

    @Override
    public String transformCondition() {
        return "("+elementGenerator.getContextName()+".get(\"" +attributeForKey("property") + "\") != null)";
    }
}
