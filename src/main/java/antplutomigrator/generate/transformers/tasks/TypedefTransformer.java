package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class TypedefTransformer extends SpecializedTaskTransformer {
    public TypedefTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() {
        log.error("Encountered " + element.getTaskName() + " at " + element.getLocation() + ". This is not yet supported...");
    }
}
