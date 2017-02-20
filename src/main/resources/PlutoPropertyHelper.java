package <pkg>;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.sugarj.common.Log;

/**
 * Created by manuel on 20.02.17.
 */
public class PlutoPropertyHelper extends PropertyHelper {

    public PropertySetter getPropertySetter() {
        return propertySetter;
    }

    public void setPropertySetter(PropertySetter propertySetter) {
        this.propertySetter = propertySetter;
    }

    public interface PropertySetter {
        void setProperty(String k, String v);
    }

    private PropertySetter propertySetter;

    @Override
    public boolean setProperty(String name, Object value, boolean verbose) {
        Log.log.log("Property " + name + " was set to " + value.toString(), Log.ALWAYS);

        if (propertySetter != null)
            propertySetter.setProperty(name, value.toString());

        return super.setProperty(name, value, verbose);
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
    public static synchronized PlutoPropertyHelper getPropertyHelper(Project project) {
        PlutoPropertyHelper helper = null;
        if (project != null) {
            helper = project.getReference(MagicNames
                    .REFID_PROPERTY_HELPER);
        }
        if (helper != null) {
            return helper;
        }

        helper = new PlutoPropertyHelper();
        helper.setProject(project);

        if (project != null) {
            project.addReference(MagicNames.REFID_PROPERTY_HELPER, helper);
        }

        return helper;
    }
}
