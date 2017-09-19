package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.ConditionTransformerFactory;
import antplutomigrator.generate.transformers.SpecializedConditionTransformer;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

public class ConditionTransformer extends SpecializedTaskTransformer {
    public ConditionTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("value", "property") &&
                this.supportsChildren(c -> supportsConditionElement(c));
    }

    @Override
    public void transform() throws RuntimeException {
        UnknownElement conditionElement = this.element.getChildren().get(0);
        SpecializedConditionTransformer conditionTransformer = ConditionTransformerFactory.getTransformer(conditionElement, elementGenerator, null);
        generator.printString("if ("+conditionTransformer.transformCondition()+") {", "}");
        generator.increaseIndentation(1);
        generator.printString("context.setProperty(\""+attributeForKey("property")+"\", "+generateToString(attributeForKey("value"))+");");
        generator.closeOneLevel();
    }

    private boolean supportsConditionElement(UnknownElement conditionElement) {
        return ConditionTransformerFactory.getTransformer(conditionElement, elementGenerator, null) != null;
    }
}
