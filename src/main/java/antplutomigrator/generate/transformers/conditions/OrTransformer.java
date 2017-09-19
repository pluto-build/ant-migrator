package antplutomigrator.generate.transformers.conditions;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public class OrTransformer extends ListBasedCondition {
    public OrTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    String getOperator() {
        return "||";
    }
}
