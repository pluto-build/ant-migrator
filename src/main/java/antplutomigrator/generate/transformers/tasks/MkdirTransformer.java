package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.UnknownElement;

public class MkdirTransformer extends SpecializedTaskTransformer {
    public MkdirTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("dir");
    }

    @Override
    public void transform() throws RuntimeException {
        // import org.apache.commons.io.FileUtils;
        // FileUtils.forceMkdir(<dir>);
        generator.addImport("org.apache.commons.io.FileUtils");
        generator.printString("FileUtils.forceMkdir("+generateToFile(attributeForKey("dir"))+");");
    }
}
