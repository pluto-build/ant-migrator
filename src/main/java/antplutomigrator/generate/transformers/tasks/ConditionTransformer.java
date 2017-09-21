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
        try {
            return this.containsOnlySupportedAttributes("value", "property") &&
                    this.supportsChildren(c -> supportsConditionElement(c));
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public void transform() throws RuntimeException {
        UnknownElement conditionElement = this.element.getChildren().get(0);
        AntIntrospectionHelper childIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), element, null, generator.getPkg(), introspectionHelper);
        antplutomigrator.generate.transformers.ConditionTransformer conditionTransformer = ConditionTransformerFactory.getTransformer(conditionElement, elementGenerator, childIntrospectionHelper);
        generator.printString("if ("+conditionTransformer.transformCondition()+") {", "}");
        generator.increaseIndentation(1);
        String value = attributeForKey("value");
        if (value == null)
            value = "true";
        generator.printString("context.setProperty(\""+attributeForKey("property")+"\", "+generateToString(value)+");");
        generator.closeOneLevel();
    }

    private boolean supportsConditionElement(UnknownElement conditionElement) {
        AntIntrospectionHelper childIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), element, null, generator.getPkg(), introspectionHelper);
        return ConditionTransformerFactory.getTransformer(conditionElement, elementGenerator, childIntrospectionHelper) != null;
    }
}
