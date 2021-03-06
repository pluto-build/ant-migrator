package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class MoveTransformer extends SpecializedTaskTransformer {
    public MoveTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
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
            //FileUtils.moveFile(src, dst);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.moveFile(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("tofile")) + ");");
        }
        if (this.containsKey("file") && this.containsKey("todir")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.moveFileToDirectory(src, dst, false);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.moveFileToDirectory(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("todir")) + ", false);");
        }
    }
}
