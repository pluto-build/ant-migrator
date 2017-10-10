package antplutomigrator.generate.transformers.fileselectors;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.conditions.ListBasedCondition;
import org.apache.tools.ant.UnknownElement;

public class OrTransformer extends ListBasedSelector {
    public OrTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    @Override
    String getOperator() {
        return "||";
    }
}
