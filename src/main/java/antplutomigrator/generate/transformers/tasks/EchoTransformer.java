package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.UnknownElement;

public class EchoTransformer extends SpecializedTaskTransformer {
    public EchoTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("message", "text", "append", "file");
    }

    @Override
    public void transform() throws RuntimeException {
        String message = null;
        if (containsKey("message"))
            message = generateToString(attributeForKey("message"));
        else {
            String text = element.getWrapper().getText().toString().trim();
            if (!text.isEmpty())
                message = generateToString(text);
        }
        if (message == null)
            throw new MigrationException(element.getTaskName() + " at " + element.getLocation().toString() + " has neither message nor text.");

        if (!containsKey("file")) {
            generator.printString("report(" + message + ");");
        } else {
            // import org.apache.commons.io.FileUtils;
            //FileUtils.writeStringToFile(file, data, append);
            generator.addImport("org.apache.commons.io.FileUtils");
            String append = "false";
            if (containsKey("append")) {
                append = generateToBoolean(attributeForKey("append"));
            }
            generator.printString("FileUtils.writeStringToFile("+generateToFile(attributeForKey("file"))+", "+message+", "+append+");");
        }
    }
}
