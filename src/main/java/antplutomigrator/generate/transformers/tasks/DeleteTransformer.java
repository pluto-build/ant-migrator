package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class DeleteTransformer extends SpecializedTaskTransformer {
    public DeleteTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("dir", "file") && this.element.getChildren() == null;
    }

    @Override
    public void transform() throws RuntimeException {
        if (containsKey("dir")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.deleteDirectory(dir);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.deleteDirectory("+generateToFile(attributeForKey("dir"))+");");
        }
        if (containsKey("file")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.deleteDirectory(dir);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.forceDelete("+generateToFile(attributeForKey("file"))+");");
        }
    }
}
