package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public abstract class SpecializedConditionTransformer extends SpecializedTaskTransformer {
    public SpecializedConditionTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public void transform() throws RuntimeException {
        throw new MigrationException("Conditions cannot be directly transformed...");
    }

    public abstract String transformCondition();

    public boolean supportsConditionElement(UnknownElement conditionElement) {
        return getConditionTransformerFor(conditionElement) != null;
    }

    public SpecializedConditionTransformer getConditionTransformerFor(UnknownElement conditionElement) {
        return ConditionTransformerFactory.getTransformer(conditionElement, elementGenerator, null);
    }
}
