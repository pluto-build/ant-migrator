package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class TempfileTransformer extends SpecializedTaskTransformer {
    public TempfileTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("property", "destdir", "prefix", "suffix", "deleteonexit", "createfile");
    }

    @Override
    public void transform() throws RuntimeException {
        if (!containsKey("property") || attributeForKey("property").length() == 0)
            throw new MigrationException("Tempfile needs a property attribute.");
        String property = generateToString(attributeForKey("property"));
        String prefix = generateToString(attributeForKey("prefix"));
        String suffix = "\"\"";
        if (containsKey("suffix"))
            suffix = generateToString(attributeForKey("suffix"));
        String destdir = generateToFile(".");
        if (containsKey("destdir"))
            destdir = generateToFile(attributeForKey("destdir"));
        String deleteOnExit = "false";
        if (containsKey("deleteonexit"))
            deleteOnExit = generateToBoolean(attributeForKey("deleteonexit"));
        String createFile = "false";
        if (containsKey("createfile"))
            createFile = generateToBoolean(attributeForKey("createfile"));

        String fileName = namingManager.getNameFor(element);
        //File tfile = FILE_UTILS.createTempFile(prefix, suffix, destDir,
        //deleteOnExit, createFile);
        generator.addImport("java.io.File");
        generator.addImport(generator.getPkg()+".lib.FileOperations");
        generator.printString("File "+fileName + " = FileOperations.createTempFile("+prefix+", "+suffix+", "+destdir+", "+ deleteOnExit + ", " + createFile+");");
        generator.printString(elementGenerator.getContextName()+".setProperty("+property+", "+fileName+".toString());");
    }
}
