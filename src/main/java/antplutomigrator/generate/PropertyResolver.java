package antplutomigrator.generate;

import antplutomigrator.generate.anthelpers.PlutoExpansionPropertyHelper;
import org.apache.tools.ant.Project;

/**
 * Created by manuel on 06.02.17.
 */
public class PropertyResolver implements Resolvable {
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
