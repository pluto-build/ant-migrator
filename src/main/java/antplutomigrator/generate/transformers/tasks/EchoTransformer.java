package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class EchoTransformer extends SpecializedTaskTransformer {
    public EchoTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("message");
    }

    @Override
    public void transform() throws RuntimeException {
        generator.printString("System.out.println(\""+resolver.getExpandedValue(element.getWrapper().getAttributeMap().get("message").toString())+"\");");
    }
}
