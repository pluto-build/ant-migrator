package generate;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

/**
 * Created by manuel on 06.02.17.
 */
public class PropertyResolver {
    private final String callerName;
    private final Project project;

    private final PlutoExpansionPropertyHelper propertyHelper;

    public PropertyResolver(Project project, String callerName) {
        this.project = project;
        this.callerName = callerName;

        this.propertyHelper = new PlutoExpansionPropertyHelper();
        this.propertyHelper.setProject(project);
        this.propertyHelper.setCallerName(callerName);
    }

    public String getExpandedValue(String unexpanded) {
        return propertyHelper.parseProperties(unexpanded).toString();
    }


}
