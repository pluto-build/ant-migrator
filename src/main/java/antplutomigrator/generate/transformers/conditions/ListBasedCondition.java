package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import org.apache.tools.ant.UnknownElement;

public abstract class ListBasedCondition extends SpecializedConditionTransformer {
    public ListBasedCondition(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    abstract String getOperator();

    @Override
    public boolean supportsElement() {
        return supportsChildren(this::supportsConditionElement);
    }

    @Override
    public String transformCondition() {
        StringBuilder result = new StringBuilder("(");
        int lineLength = 0;
        for (UnknownElement c: element.getChildren()) {
            if (lineLength > 80) {
                result.append(generator.getIndentString()).append("  \n");
                lineLength = 0;
            }
            if (!result.toString().equals("("))
                result.append(" ").append(getOperator()).append(" ");
            String condition = getConditionTransformerFor(c).transformCondition();
            lineLength += condition.length();
            result.append(condition);
        }
        return result + ")";
    }
}
