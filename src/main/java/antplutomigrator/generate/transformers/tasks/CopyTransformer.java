package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.DefaultTaskTransformer;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.UnknownElement;

/**
 * Emits idiomatic code for copying files/directories
 * NOTE: This does not obey the overwrite rules... It will always overwrite files!
 */
public class CopyTransformer extends SpecializedTaskTransformer {
    public CopyTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return this.containsOnlySupportedAttributes("file", "tofile", "todir", "overwrite", "flatten") && this.supportsChildren(c -> c.getTaskName().equals("fileset"));
    }

    @Override
    public void transform() throws RuntimeException {
        if (this.containsKey("file") && this.containsKey("tofile")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.copyFile(src, dst);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.copyFile(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("tofile")) + ");");
        }
        if (this.containsKey("file") && this.containsKey("todir")) {
            //import org.apache.commons.io.FileUtils;
            //FileUtils.copyFileToDirectory(src, dst, false);
            generator.addImport("org.apache.commons.io.FileUtils");
            generator.printString("FileUtils.copyFileToDirectory(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("todir")) + ", false);");
        }
        if (this.element.getChildren() != null && this.containsKey("todir")) {
            for (UnknownElement fileset: this.element.getChildren()) {
                if (!fileset.getTaskName().equals("fileset"))
                    throw new MigrationException("Copy did contain an unexpected child.");

                DefaultTaskTransformer defaultTaskTransformer = new DefaultTaskTransformer(fileset, this.elementGenerator, AntIntrospectionHelper.getInstanceFor(introspectionHelper.getProject(), fileset, namingManager.getNameFor(fileset), introspectionHelper.getPkg(), null));
                defaultTaskTransformer.transform();

                String flatten = null;
                if (containsKey("flatten"))
                    flatten = generateToBoolean(attributeForKey("flatten"));

                generator.addImport(generator.getPkg()+".lib.FileOperations");
                generator.printString("FileOperations.copy("+generateToFile(this.attributeForKey("todir"))+", "+namingManager.getNameFor(fileset)+((flatten != null)?(", "+flatten):"")+");");
            }
        }
    }
}
