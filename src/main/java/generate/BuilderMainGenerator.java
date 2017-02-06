package generate;

import com.sun.xml.internal.messaging.saaj.soap.StringDataContentHandler;
import utils.StringUtils;

/**
 * Created by manuel on 06.02.17.
 */
public class BuilderMainGenerator extends Generator {

    private final String pkg;
    private final String name;
    private final String defTarget;

    public BuilderMainGenerator(String pkg, String name, String defTarget) {
        this.name = name;
        this.pkg = pkg;
        this.defTarget = defTarget;
    }

    @Override
    public void generatePrettyPrint() {
        super.generatePrettyPrint();

        this.printString("package " + pkg + ";");
        this.printString("import build.pluto.builder.BuildManagers;");
        this.printString("import build.pluto.builder.BuildRequest;");

        this.printString("public class "+ StringUtils.capitalize(name)+" {", "}");
        this.increaseIndentation(1);

        this.printString("public static void main(String[] args) throws Throwable {", "}");
        this.increaseIndentation(1);
        this.printString(StringUtils.capitalize(name) + "Input input = new " + StringUtils.capitalize(name) + "Input();");
        this.printString("BuildManagers.build(new BuildRequest<>("+StringUtils.capitalize(defTarget)+"Builder.factory, input));");

        this.closeOneLevel();
        this.closeOneLevel();
    }
}