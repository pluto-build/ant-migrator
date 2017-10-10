package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.DefaultTaskTransformer;
import antplutomigrator.generate.transformers.FileSelectorTransformer;
import antplutomigrator.generate.transformers.FileSelectorTransformerFactory;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.UnknownElement;

import java.util.ArrayList;
import java.util.List;

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
                AntIntrospectionHelper fileSetIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), fileset, null, generator.getPkg(), introspectionHelper);
                if (!fileset.getTaskName().equals("fileset"))
                    throw new MigrationException("Copy did contain an unexpected child.");

                //DefaultTaskTransformer defaultTaskTransformer = new DefaultTaskTransformer(fileset, this.elementGenerator, AntIntrospectionHelper.getInstanceFor(introspectionHelper.getProject(), fileset, namingManager.getNameFor(fileset), introspectionHelper.getPkg(), null));
                //defaultTaskTransformer.transform();

                if (fileset.getChildren() == null)
                    throw new MigrationException("Fileset need children.");

                List<UnknownElement> includes = new ArrayList<>();
                List<UnknownElement> excludes = new ArrayList<>();
                for (UnknownElement fileSetChild: fileset.getChildren()) {
                    if (!fileSetChild.getTaskName().equals("exclude"))
                        includes.add(fileSetChild);
                    else
                        excludes.add(fileSetChild);
                }
                String includeTransformed = "";
                if (includes.size() > 0)
                {
                    // handle all includes by putting them in an or
                    UnknownElement or = new UnknownElement("or");
                    or.setTaskName("or");
                    for (UnknownElement include: includes)
                        or.addChild(include);

                    AntIntrospectionHelper orIntroSpectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), or, null, generator.getPkg(), fileSetIntrospectionHelper);
                    FileSelectorTransformer orTransformer = FileSelectorTransformerFactory.getTransformer(or, elementGenerator, orIntroSpectionHelper, generateToFile(fileset.getWrapper().getAttributeMap().get("dir").toString()), "s");
                    includeTransformed = orTransformer.transformFileSelector();
                }

                String excludesTransformed = "";
                if (excludes.size() > 0)
                {
                    // handle all excludes by putting them in an and
                    UnknownElement or = new UnknownElement("or");
                    or.setTaskName("or");
                    for (UnknownElement exclude: excludes) {
                        // Negation happens outside!
                        exclude.setTaskName("include");
                        or.addChild(exclude);
                    }

                    AntIntrospectionHelper andIntroSpectionHelper = AntIntrospectionHelper.getInstanceFor(elementGenerator.getProject(), or, null, generator.getPkg(), fileSetIntrospectionHelper);
                    FileSelectorTransformer andTransformer = FileSelectorTransformerFactory.getTransformer(or, elementGenerator, andIntroSpectionHelper, generateToFile(attributeForKey(fileset, "dir")), "s");
                    excludesTransformed = andTransformer.transformFileSelector();
                }

                String transformed = "";
                if (!includeTransformed.equals(""))
                    transformed = includeTransformed;
                if (!excludesTransformed.equals(""))
                {
                    if (!transformed.equals(""))
                        transformed += " && !";
                    transformed += excludesTransformed;
                }

                String flatten = null;
                if (containsKey("flatten"))
                    flatten = generateToBoolean(attributeForKey("flatten"));

                generator.addImport(generator.getPkg()+".lib.FileOperations");
                generator.printString("FileOperations.copy("+generateToFile(this.attributeForKey(fileset, "dir"))+", "+generateToFile(this.attributeForKey("todir"))+", s -> "+transformed+((flatten != null)?(", "+flatten+", true"):"")+");");
            }
        }
        /*if (this.element.getChildren() != null && this.containsKey("todir")) {
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
        }*/
    }
}
