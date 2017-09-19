package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public class NotTransformer extends SpecializedConditionTransformer {
    public NotTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return element.getChildren() != null && element.getChildren().size() == 1 && getConditionTransformerFor(element.getChildren().get(0)) != null;
    }

    @Override
    public String transformCondition() {
        SpecializedConditionTransformer conditionTransformer = getConditionTransformerFor(element.getChildren().get(0));
        return "!"+conditionTransformer.transformCondition();
    }
}
