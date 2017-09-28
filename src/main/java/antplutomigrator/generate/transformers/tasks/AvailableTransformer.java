package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class AvailableTransformer extends SpecializedTaskTransformer {
    public AvailableTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("file", "property", "value");
    }

    @Override
    public void transform() throws RuntimeException {
        generator.printString("if ("+generateToFile(attributeForKey("file"))+".exists())");
        generator.increaseIndentation(1);
        String value = "true";
        if (containsKey("value"))
            value = attributeForKey("value");
        generator.printString(elementGenerator.getContextName()+".setProperty(\""+attributeForKey("property")+"\", "+generateToString(value)+");");
        generator.increaseIndentation(-1);
    }
}
