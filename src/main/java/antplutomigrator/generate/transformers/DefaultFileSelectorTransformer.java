package antplutomigrator.generate.transformers;

import antplutomigrator.generate.BuilderGenerator;
import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.Statistics;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;

import java.io.File;

public class DefaultFileSelectorTransformer extends FileSelectorTransformer {
    public DefaultFileSelectorTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper, String baseDir, String fileName) {
        super(element, elementGenerator, introspectionHelper, baseDir, fileName);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() {
        Statistics.getInstance().defaultGenerated(element);
        //generator.printString("final " + ((BuilderGenerator)generator).getInputName() + " f" + elementGenerator.getContextName() + " = " + elementGenerator.getContextName()+";");
        if (!elementGenerator.isNoConstructor()) {
            ConstructorTaskTransformer constructorTaskTransformer = new ConstructorTaskTransformer(element, elementGenerator, introspectionHelper);
            constructorTaskTransformer.transform();
        }
        if (!elementGenerator.isOnlyConstructors()) {
            ConfigureTaskTransformer configureTaskTransformer = new ConfigureTaskTransformer(element, elementGenerator, introspectionHelper);
            configureTaskTransformer.transform();
        }
    }

    @Override
    public String transformFileSelector() {
        transform();
        // TODO: very hacky
       return namingManager.getNameFor(element) + ".isSelected("+baseDir+", "+fileName+", new File("+baseDir+", "+fileName+"))";
    }
}
