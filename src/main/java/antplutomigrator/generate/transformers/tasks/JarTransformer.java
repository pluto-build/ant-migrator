package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.FileSetUtils;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

import java.util.Map;

public class JarTransformer extends SpecializedTaskTransformer {
    public JarTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("destfile", "jarfile" , "basedir", "manifest") && supportsChildren(c -> c.getTaskName().equals("fileset") || c.getTaskName().equals("zipfileset"));
    }

    @Override
    public void transform() throws RuntimeException {
        generator.addImport(generator.getPkg()+".lib.FileOperations");

        String dest;
        if (this.containsKey("destfile")) {
            dest = attributeForKey("destfile");
        } else if (this.containsKey("jarfile")) {
            dest = attributeForKey("jarfile");
        } else {
            throw new MigrationException("You need to use at least destfile or jarfile (deprecated)");
        }
        generator.addImport("java.util.jar.JarOutputStream");
        generator.addImport("java.io.FileOutputStream");
        String outputStream = "new FileOutputStream(" + generateToFile(dest) + ")";
        if (this.containsKey("manifest")) {
            String manifestFile = generateToFile(attributeForKey(("manifest")));
            String manifest = "FileOperations.getManifestFile(" + manifestFile + ")";
            generator.printString("try(JarOutputStream out = new JarOutputStream("+ outputStream +", " + manifest + ")) {", "}");
        } else {
            generator.printString("try(JarOutputStream out = new JarOutputStream("+ outputStream +")) {", "}");
        }
        generator.increaseIndentation(1);
        if (this.containsKey("basedir")) {
        } else if (this.element.getChildren() != null) {
            for (UnknownElement fileset : this.element.getChildren()) {
                Map<String, Object> attributes = fileset.getWrapper().getAttributeMap();
                if (fileset.getTaskName().equals("zipfileset")) {
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
