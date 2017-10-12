package antplutomigrator.generate.transformers.fileselectors;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public class FilenameTransformer extends IncludeTransformer {
    public FilenameTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }
}
