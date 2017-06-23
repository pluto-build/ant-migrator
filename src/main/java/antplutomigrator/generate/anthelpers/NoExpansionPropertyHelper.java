package antplutomigrator.generate.anthelpers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

/**
 * Created by manuel on 06.02.17.
 */
public class NoExpansionPropertyHelper extends PropertyHelper {
    public NoExpansionPropertyHelper() {
    }

    @Override
    public Object parseProperties(String value) throws BuildException {
        return value;
    }

    public Object reallyParseProperties(String value) throws BuildException {
        return super.parseProperties(value);
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
    public static synchronized NoExpansionPropertyHelper getPropertyHelper(Project project) {
        NoExpansionPropertyHelper helper = null;
        if (project != null) {
            PropertyHelper helper1 = project.getReference(MagicNames
                    .REFID_PROPERTY_HELPER);
            if (helper1 instanceof NoExpansionPropertyHelper)
                helper = (NoExpansionPropertyHelper) helper1;
        }
        if (helper != null) {
            return helper;
        }

        helper = new NoExpansionPropertyHelper();
        helper.setProject(project);

        if (project != null) {
            project.addReference(MagicNames.REFID_PROPERTY_HELPER, helper);
        }

        return helper;
    }
}
