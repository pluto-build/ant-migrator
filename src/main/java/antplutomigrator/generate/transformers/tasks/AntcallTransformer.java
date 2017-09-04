package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import antplutomigrator.utils.StringUtils;
import org.apache.tools.ant.UnknownElement;

public class AntcallTransformer extends SpecializedTaskTransformer {
    public AntcallTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return true;
    }

    @Override
    public void transform() {
        if (elementGenerator.isOnlyConstructors())
            return;

        String depName = StringUtils.capitalize(elementGenerator.getNamingManager().getClassNameFor(introspectionHelper.getAttributeMap().get("target").toString()));
        //generator.printString(this.getInputName() + " " + StringUtils.decapitalize(depName) + "Input = new " + this.getInputName() + "();");
        String antCallContextName = elementGenerator.getNamingManager().getNameFor("antcallContext");
        generator.printString(elementGenerator.getInputName() + " " + antCallContextName + " = context.clone();");

        if (introspectionHelper.getElement().getChildren() != null) {
            for (UnknownElement child : introspectionHelper.getElement().getChildren()) {
                if (child.getTaskName().equals("param")) {
                    generator.printString(antCallContextName + ".setProperty(\"" + elementGenerator.getResolver().getExpandedValue(child.getWrapper().getAttributeMap().get("name").toString()) + "\", \"" + elementGenerator.getResolver().getExpandedValue(child.getWrapper().getAttributeMap().get("value").toString()) + "\");");
                } else {
                    log.warn("While migrating antcall " + depName + " couldn't deal with child " + child.getTaskName());
                }
            }
        }

        if (!elementGenerator.isInMacro())
            generator.printString("antCall(" + depName + "Builder.factory, "+antCallContextName+");");
        else {
            String pkg = introspectionHelper.getPkg();
            if (pkg.endsWith(".macros"))
                pkg = pkg.substring(0, pkg.lastIndexOf(".macros"));
            generator.addImport( pkg + "." + depName + "Builder");
            generator.printString("builder.antCall(" + depName + "Builder.factory, "+antCallContextName+");");
        }
    }
}
