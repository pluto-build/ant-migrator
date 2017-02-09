package generate.anthelpers;

import org.apache.tools.ant.PropertyHelper;

/**
 * Created by manuel on 06.02.17.
 */
public class PlutoPropertyEvaluator implements PropertyHelper.PropertyEvaluator {
    @Override
    public Object evaluate(String property, PropertyHelper propertyHelper) {
        if (propertyHelper instanceof PlutoExpansionPropertyHelper) {
            PlutoExpansionPropertyHelper plutoExpansionPropertyHelper = (PlutoExpansionPropertyHelper)propertyHelper;

            return "\" + " + plutoExpansionPropertyHelper.callerName + ".get(\"" + property + "\") + \"";
        }
        return null;
    }
}
