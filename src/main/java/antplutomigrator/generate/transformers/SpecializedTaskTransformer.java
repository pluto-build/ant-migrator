package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

import java.util.Arrays;

public abstract class SpecializedTaskTransformer extends Transformer {
    public SpecializedTaskTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
        if (!this.getClass().getSimpleName().equalsIgnoreCase(element.getTaskName() + "Transformer"))
            throw new MigrationException("Taskname "+this.getClass().getSimpleName()+ " doesn't match for " + element.getTaskName());
    }
}
