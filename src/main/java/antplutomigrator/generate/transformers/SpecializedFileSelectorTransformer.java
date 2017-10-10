package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

public abstract class SpecializedFileSelectorTransformer extends FileSelectorTransformer {
    public SpecializedFileSelectorTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    @Override
    public void transform() throws RuntimeException {
        throw new MigrationException("Conditions cannot be directly transformed...");
    }

    public boolean supportsFileSelectorElement(UnknownElement conditionElement) {
        return getFileSelectorTransformerFor(conditionElement) != null;
    }

    public FileSelectorTransformer getFileSelectorTransformerFor(UnknownElement element) {
        AntIntrospectionHelper childIntrospectionHelper = null;
            childIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), element, null, generator.getPkg(), introspectionHelper);
        return FileSelectorTransformerFactory.getTransformer(element, elementGenerator, childIntrospectionHelper, baseDir, fileName);
    }
}
