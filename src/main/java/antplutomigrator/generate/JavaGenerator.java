package antplutomigrator.generate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 14.02.17.
 */
public class JavaGenerator extends Generator {
    private final String pkg;
    private List<String> imports = new ArrayList<>();


    public String getPkg() {
        return pkg;
    }

    public List<String> getImports() {
        return imports;
    }

    public JavaGenerator(String pkg) {
        this.pkg = pkg;
    }

    public void addImport(String cls) {
        if (!imports.contains(cls))
            imports.add(cls);
    }

    private String getImportPrettyPrint() {
        StringBuilder sb = new StringBuilder();
        for (String anImport : getImports()) {
            sb.append("import ").append(anImport).append(";\n");
        }
        return sb.toString();
    }

    public void generatePrettyPrint() {
        super.generatePrettyPrint();
        printString("package " + getPkg() + ";\n");
        this.printString("");
        printLater(() -> getImportPrettyPrint());
        this.printString("");
    }
}
