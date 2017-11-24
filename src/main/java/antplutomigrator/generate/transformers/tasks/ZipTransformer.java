package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.FileSetUtils;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

import java.io.OutputStream;
import java.util.Map;

public class ZipTransformer extends SpecializedTaskTransformer {
    public ZipTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("basedir", "destfile", "zipfile") && this.supportsChildren(c -> c.getTaskName().equals("fileset") || c.getTaskName().equals("zipfileset"));
    }

    @Override
    public void transform() throws RuntimeException {
        generator.addImport(generator.getPkg()+".lib.FileOperations");

        String dest;
        if (this.containsKey("destfile")) {
            dest = attributeForKey("destfile");
        } else if (this.containsKey("zipfile")) {
            dest = attributeForKey("zipfile");
        } else {
            throw new MigrationException("You need to use at least destfile or zipfile (deprecated)");
        }
        generator.addImport("org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream");
        generator.addImport("org.apache.commons.compress.archivers.ArchiveOutputStream");
        // introduces this scope to avoid name clashes for different transformer runs
        generator.printString("try(ArchiveOutputStream out = new ZipArchiveOutputStream(" + generateToFile(dest) + ")) {", "}");
        generator.increaseIndentation(1);
        if (this.containsKey("basedir")) {
            generator.printString("FileOperations.packageFiles(out, " + generateToFile(attributeForKey("basedir")) + ", \"\", s -> true);");
        } else if (this.element.getChildren() != null) {
            for (UnknownElement fileset : this.element.getChildren()) {
                Map<String, Object> attributes = fileset.getWrapper().getAttributeMap();
                if (fileset.getTaskName().equals("zipfileset")) {
                    // TODO includes and excludes attributes in zipfileset tag
                    if (attributes.containsKey("dir")) {
                        String dir = generateToFile(attributeForKey(fileset, "dir"));
                        String predicate = FileSetUtils.getPredicateFromZipFileSet(fileset, this);
                        String prefix = generateToString(attributeForKey(fileset, "prefix"));
                        prefix = prefix.equals("null") ? "\"\"" : prefix;
                        generator.printString("FileOperations.packageFiles(out, " + dir + ", " + prefix + ", s -> " + predicate + ");");
                    } else if (attributes.containsKey("file") && attributes.containsKey("fullpath") && fileset.getChildren() == null) {
                        // only look at one file
                        String file = generateToFile(attributeForKey(fileset, "file"));
                        String fullpath = generateToString(attributeForKey(fileset, "fullpath"));
                        generator.printString("FileOperations.addFileToArchive(out, " + file + ", "+ fullpath +");");
                    } else if (attributes.containsKey("src")) {
                        // TODO extract zipfile at path src and include content into new zipfile (Not used in tomcat script)
                        throw new UnsupportedOperationException("src is not currently not supported");
                    } else {
                        throw new MigrationException("src, dir or file with fullpath inside of zipfileset is not defined");
                    }
                } else if (fileset.getTaskName().equals("fileset")) {
                    if (attributes.containsKey("dir")) {
                        String dir = generateToFile(attributeForKey(fileset, "dir"));
                        String predicate = FileSetUtils.getPredicateFromFileSet(fileset, this);
                        generator.printString("FileOperations.packageFiles(out, " + dir + ", \"\",  s -> " + predicate + ");");
                    } else {
                        throw new MigrationException("dir inside of fileset is not defined");
                    }
                }
            }
        }
        generator.closeOneLevel();
    }
}
