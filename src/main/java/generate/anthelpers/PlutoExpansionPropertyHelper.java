package generate.anthelpers;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by manuel on 06.02.17.
 */
public class PlutoExpansionPropertyHelper extends PropertyHelper {
    public Map<String, String> plutoExpandedValues = new HashMap<>();

    protected String callerName = "this";

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public PlutoExpansionPropertyHelper() {
        add(new PlutoPropertyEvaluator());
    }

    /**
     * Factory method to create a property processor.
     * Users can provide their own or replace it using "ant.PropertyHelper"
     * reference. User tasks can also add themselves to the chain, and provide
     * dynamic properties.
     *
     * @param project the project for which the property helper is required.
     *
     * @return the project's property helper.
     */
    public static synchronized PlutoExpansionPropertyHelper getPropertyHelper(Project project) {
        PlutoExpansionPropertyHelper helper = null;
        if (project != null) {
            PropertyHelper helper1 = project.getReference(MagicNames
                    .REFID_PROPERTY_HELPER);
            if (helper1 instanceof PlutoExpansionPropertyHelper)
                helper = (PlutoExpansionPropertyHelper) helper1;
        }
        if (helper != null) {
            return helper;
        }

        helper = new PlutoExpansionPropertyHelper();
        helper.setProject(project);

        if (project != null) {
            project.addReference(MagicNames.REFID_PROPERTY_HELPER, helper);
        }

        return helper;
    }
}
