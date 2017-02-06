package generate;

import java.util.Map;

/**
 * Created by manuel on 13.12.16.
 */
public class BuilderInputGenerator extends Generator {

    private final String name;
    //private final VariableManager manager;
    private boolean includeEmpty = true;

    public BuilderInputGenerator(String name) {
        this.name = name;
    }

    /**
     *
     * @param name
     * @param includeEmpty set this to false, to omit empty variables in the output. This can result in wrong values, as empty variables can override environment variables...
     */
    public BuilderInputGenerator(String name, boolean includeEmpty) {
        this(name);
        this.includeEmpty = includeEmpty;
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        /*this.printString("public class " + name + " extends Input {", "}");
        this.increaseIndentation(1);
        this.printString("public String get(String v) {","}");
        this.increaseIndentation(1);
        this.printString("switch (v) {", "}");
        this.increaseIndentation(1);
        for (Map.Entry<String, Variable> variable: manager.getVariables().entrySet()) {
            variable.getValue().expandPluto(manager, "this");
            if (includeEmpty || !variable.getValue().getPlutoExpandedValue().isEmpty()) {
                this.printString("case \"" + variable.getKey() + "\":", "");
                this.increaseIndentation(1);
                this.printString("return \"" + variable.getValue().getPlutoExpandedValue() + "\";");
                this.closeOneLevel();
            }
        }
        this.printString("default:\n" +
                "  String envValue = System.getenv(v);\n" +
                "  if (envValue != null)\n" +
                "    return envValue;\n" +
                "  return \"\";");
        this.closeOneLevel();
        this.closeOneLevel();
        this.closeOneLevel();*/
    }
}
