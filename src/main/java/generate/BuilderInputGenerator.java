package generate;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import utils.StringUtils;

import java.util.Map;

/**
 * Created by manuel on 13.12.16.
 */
public class BuilderInputGenerator extends Generator {

    private final String pkg;
    private final String name;
    private final Project project;
    private boolean includeEmpty = true;
    private PropertyResolver resolver;

    public BuilderInputGenerator(String pkg, String name, Project project) {
        this.name = name;
        this.pkg = pkg;
        this.project = project;
        this.resolver = new PropertyResolver(project, "this");
    }

    /**
     *
     * @param name
     * @param includeEmpty set this to false, to omit empty variables in the output. This can result in wrong values, as empty variables can override environment variables...
     */
    public BuilderInputGenerator(String pkg, String name, Project project, boolean includeEmpty) {
        this(pkg, name, project);
        this.includeEmpty = includeEmpty;
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.printString("package " + pkg + ";");
        this.printString("import java.io.Serializable;");

        this.printString("public class " + name + " implements Serializable {", "}");
        this.increaseIndentation(1);

        this.printString("public String get(String v) {","}");
        this.increaseIndentation(1);
        this.printString("switch (v) {", "}");
        this.increaseIndentation(1);
        //PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        //propertyHelper.getProperties();
        project.getProperties().forEach(
                (k, v) -> {
                    this.printString("case \"" + k + "\":", "");
                    this.increaseIndentation(1);
                    this.printString("return " + resolver.getExpandedValue(StringUtils.javaPrint(v.toString())) + ";");
                    this.closeOneLevel();
                }
        );
        this.printString("default:\n" +
                "  String envValue = System.getenv(v);\n" +
                "  if (envValue != null)\n" +
                "    return envValue;\n" +
                "  return \"\";");
        this.closeOneLevel();
        this.closeOneLevel();
        this.closeOneLevel();
    }
}
