package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.Statistics;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public class DefaultConditionTransformer extends ConditionTransformer {
    public DefaultConditionTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() {
        Statistics.getInstance().defaultGenerated(element);
        if (!elementGenerator.isNoConstructor()) {
            ConstructorTaskTransformer constructorTaskTransformer = new ConstructorTaskTransformer(element, elementGenerator, introspectionHelper);
            constructorTaskTransformer.transform();
        }
        if (!elementGenerator.isOnlyConstructors()) {
            ConfigureTaskTransformer configureTaskTransformer = new ConfigureTaskTransformer(element, elementGenerator, introspectionHelper);
            configureTaskTransformer.transform();
        }
    }

    @Override
    public String transformCondition() {
        transform();
        return namingManager.getNameFor(element) + ".eval()";
    }
}
