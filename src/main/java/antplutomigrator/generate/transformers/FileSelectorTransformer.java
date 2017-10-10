package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public abstract class FileSelectorTransformer extends Transformer {
    protected final String baseDir;
    protected final String fileName;

    public FileSelectorTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper);
        this.baseDir = baseDir;
        this.fileName = fileName;
    }

    public abstract String transformFileSelector();
}
