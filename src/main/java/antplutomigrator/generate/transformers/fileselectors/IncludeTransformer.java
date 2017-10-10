package antplutomigrator.generate.transformers.fileselectors;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedFileSelectorTransformer;
import org.apache.tools.ant.UnknownElement;

public class IncludeTransformer extends SpecializedFileSelectorTransformer {
    public IncludeTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("name");
    }

    @Override
    public String transformFileSelector() {
        //import org.apache.tools.ant.types.selectors.SelectorUtils;
        //SelectorUtils.matchPath("**/*.js", s);
        generator.addImport("org.apache.tools.ant.types.selectors.SelectorUtils");
        return "SelectorUtils.matchPath("+generateToString(attributeForKey("name"))+", "+fileName+")";
    }
}
