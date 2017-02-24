package <pkg>;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.sugarj.common.Log;

/**
 * Created by manuel on 20.02.17.
 */
public class PlutoPropertyHelper extends PropertyHelper {

    public PlutoPropertyHelper() {
        add(new PropertyHelper.PropertySetter() {
            @Override
            public boolean setNew(String property, Object value, PropertyHelper propertyHelper) {
                Log.log.log("New property " + property + " was set to " + value.toString(), Log.ALWAYS);
                propertyInteractor.setProperty(property, String.valueOf(value));
                return true;
            }

            @Override
            public boolean set(String property, Object value, PropertyHelper propertyHelper) {
                Log.log.log("Property " + property + " was set to " + value.toString(), Log.ALWAYS);
                propertyInteractor.setProperty(property, String.valueOf(value));
                return true;
            }
        });
        add(new PropertyEvaluator() {
            @Override
            public Object evaluate(String property, PropertyHelper propertyHelper) {
                Log.log.log("Property " + property + " was retrieved...", Log.ALWAYS);
                return propertyInteractor.get(property);
            }
        });
    }

    public PropertyInteractor getPropertyInteractor() {
        return propertyInteractor;
    }

    public void setPropertyInteractor(PropertyInteractor propertyInteractor) {
        this.propertyInteractor = propertyInteractor;
    }

    public interface PropertyInteractor {
        void setProperty(String k, String v);
        String get(String k);
    }

    private PropertyInteractor propertyInteractor;


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
            try {
                helper = project.getReference(MagicNames
                        .REFID_PROPERTY_HELPER);
            } catch (ClassCastException e) { }
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
