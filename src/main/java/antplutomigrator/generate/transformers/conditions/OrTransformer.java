package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public class OrTransformer extends SpecializedConditionTransformer {
    public OrTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return supportsChildren(c -> supportsConditionElement(c));
    }

    @Override
    public String transformCondition() {
        String result = "(";
        for (UnknownElement c: element.getChildren()) {
            if (!result.equals("("))
                result += " || ";
            result += getConditionTransformerFor(c).transformCondition();
        }
        return result + ")";
    }
}
