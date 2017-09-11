package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
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
            message = attributeForKey("message");
        else {
            String text = element.getWrapper().getText().toString().trim();
            if (!text.isEmpty())
                message = resolver.getExpandedValue(text);
        }
        if (message == null)
            throw new MigrationException(element.getTaskName() + " at " + element.getLocation().toString() + " has neither message nor text.");

        if (!containsKey("file")) {
            generator.addImport("org.sugarj.common.Log");
            generator.printString("Log.log.log(\"" + message + "\", Log.ALWAYS);");
        } else {
            // import org.apache.commons.io.FileUtils;
            //FileUtils.writeStringToFile(file, data, append);
            generator.addImport("org.apache.commons.io.FileUtils");
            String append = "false";
            if (containsKey("append")) {
                generator.addImport("org.apache.tools.ant.Project");
                // TODO: Handling for "nice" booleans e.g. minimize Project.toBoolean("false") to false ...
                append = "Project.toBoolean(\""+attributeForKey("append")+"\")";
            }
            generator.printString("FileUtils.writeStringToFile("+elementGenerator.getContextName()+".resolveFile(\""+attributeForKey("file")+"\"), \""+message+"\", "+append+");");
        }
    }
}
