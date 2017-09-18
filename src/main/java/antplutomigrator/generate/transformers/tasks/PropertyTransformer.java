package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class PropertyTransformer extends SpecializedTaskTransformer {
    public PropertyTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("name", "value");
    }

    @Override
    public void transform() throws RuntimeException {
        generator.printString("context.setProperty(\""+attributeForKey("name")+"\", "+generateToString(attributeForKey("value"))+");");
    }
}
