package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class GetTransformer extends SpecializedTaskTransformer {
    public GetTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        // Currently httpusecaches is ignored
        return containsOnlySupportedAttributes("src", "dest", "httpusecaches");
    }

    @Override
    public void transform() throws RuntimeException {
        generator.addImport(generator.getPkg()+".lib.FileOperations");
        generator.addImport("java.net.URL");
        if (!containsKey("src") || !containsKey("dest"))
            throw new MigrationException("Get has to have a src and dest");
        generator.printString("FileOperations.downloadFile(new URL("+generateToString(attributeForKey("src"))+"), "+generateToFile(attributeForKey("dest"))+");");
    }
}
