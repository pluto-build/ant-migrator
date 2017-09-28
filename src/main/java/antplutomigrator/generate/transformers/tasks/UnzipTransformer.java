package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class UnzipTransformer extends SpecializedTaskTransformer {
    public UnzipTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("src", "dest");
    }

    @Override
    public void transform() throws RuntimeException {
        generator.addImport(generator.getPkg()+".lib.FileOperations");
        generator.printString("FileOperations.unzip("+generateToFile(attributeForKey("src"))+", "+generateToFile(attributeForKey("dest"))+");");
    }
}
