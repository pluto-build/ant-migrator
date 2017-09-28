package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.UnknownElement;

public class CopyTransformer extends SpecializedTaskTransformer {
    public CopyTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("file", "tofile", "todir") && this.element.getChildren() == null;
    }

    @Override
    public void transform() throws RuntimeException {
        if (this.containsKey("file") && this.containsKey("tofile")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.copyFile(src, dst);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.copyFile(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("tofile")) + ");");
        }
        if (this.containsKey("file") && this.containsKey("todir")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.copyFileToDirectory(src, dst, false);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.copyFileToDirectory(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("todir")) + ", false);");
        }
    }
}
