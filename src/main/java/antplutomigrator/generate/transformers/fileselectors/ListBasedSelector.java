package antplutomigrator.generate.transformers.fileselectors;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import antplutomigrator.generate.transformers.SpecializedFileSelectorTransformer;
import org.apache.tools.ant.UnknownElement;

public abstract class ListBasedSelector extends SpecializedFileSelectorTransformer {
    public ListBasedSelector(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    abstract String getOperator();

    @Override
    public boolean supportsElement() {
        return supportsChildren(this::supportsFileSelectorElement);
    }

    @Override
    public String transformFileSelector() {
        if (element.getChildren().size() == 1)
            return getFileSelectorTransformerFor(element.getChildren().get(0)).transformFileSelector();
        StringBuilder result = new StringBuilder("(");
        for (UnknownElement c: element.getChildren()) {
            String condition = getFileSelectorTransformerFor(c).transformFileSelector();
            if (!condition.isEmpty()) {
                if (!result.toString().equals("("))
                    result.append(" ").append(getOperator()).append(" ");
                result.append(condition);
            }
        }
        if (result.equals("("))
            return "";
        return result + ")";
    }
}
