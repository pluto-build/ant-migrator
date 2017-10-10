package antplutomigrator.generate.transformers.fileselectors;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.ConditionTransformer;
import antplutomigrator.generate.transformers.FileSelectorTransformer;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import antplutomigrator.generate.transformers.SpecializedFileSelectorTransformer;
import org.apache.tools.ant.UnknownElement;

public class NotTransformer extends SpecializedFileSelectorTransformer {
    public NotTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    @Override
    public boolean supportsElement() {
        return element.getChildren() != null && element.getChildren().size() == 1 && getFileSelectorTransformerFor(element.getChildren().get(0)) != null;
    }

    @Override
    public String transformFileSelector() {
        FileSelectorTransformer fileSelectorTransformer = getFileSelectorTransformerFor(element.getChildren().get(0));
        String transformed = fileSelectorTransformer.transformFileSelector();
        if (transformed.isEmpty())
            return transformed;
        return "!"+transformed;
    }
}
