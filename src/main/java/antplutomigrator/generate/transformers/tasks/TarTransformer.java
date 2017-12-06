package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.FileSetUtils;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

import java.util.Map;

public class TarTransformer extends SpecializedTaskTransformer {
    public static final int DEFAULT_FILE_MODE = 644;
    public static final int DEFAULT_DIR_MODE = 755;

    public TarTransformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        super(element, elementGenerator, introspectionHelper);
    }

    @Override
    public boolean supportsElement() {
        return containsOnlySupportedAttributes("longfile", "compression", "tarfile", "basedir", "destfile") && supportsChildren(c -> c.getTaskName().equals("fileset") || c.getTaskName().equals("tarfileset"));
    }

    @Override
    public void transform() throws RuntimeException {
        generator.addImport(generator.getPkg()+".lib.FileOperations");
        String dest;
        if (this.containsKey("destfile")) {
            dest = attributeForKey("destfile");
        } else if (this.containsKey("tarfile")) {
            dest = attributeForKey("tarfile");
        } else {
            throw new MigrationException("You need to use at least destfile or tarfile (deprecated)");
        }
        generator.addImport("java.io.FileOutputStream");
        generator.addImport("org.apache.commons.compress.archivers.tar.TarArchiveOutputStream");
        generator.addImport("org.apache.commons.compress.archivers.ArchiveOutputStream");
        // introduces this scope to avoid name clashes for different transformer runs
        // set compression
        String destFile = generateToFile(dest);
        String compression = getOptionalAttribute(this.element, "compression");
        destFile = "new FileOutputStream(" + destFile + ")";
        if (compression.equals("") || compression.equals("none")) {
        } else if (compression.equals("gzip")) {
            generator.addImport("org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream");
            destFile = "new GzipCompressorOutputStream(" + destFile + ")";
        } else if (compression.equals("bzip2")) {
            generator.addImport("org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream");
            destFile = "new BZip2CompressorOutputStream(" + destFile + ")";
        } else {
            throw new MigrationException("Unsupported compression flag was used");
        }
        generator.printString("try(TarArchiveOutputStream out = new TarArchiveOutputStream(" + destFile + ")) {", "}");
        // set longfile mode
        String longfile = getOptionalAttribute(this.element, "longfile");
        int longfileCode = getLongfileCode(longfile);
        if (longfileCode == -1) {
            throw new MigrationException("Unsupported longfile mode used");
        }
        generator.increaseIndentation(1);
        generator.printString("out.setLongFileMode(" + longfileCode + ");");
        if (this.containsKey("basedir")) {
            generator.printString("FileOperations.packageFiles(out, " + generateToFile(attributeForKey("basedir")) + ", \"\", s -> true);");
        } else if (this.element.getChildren() != null) {
            for (UnknownElement fileset : this.element.getChildren()) {
                Map<String, Object> attributes = fileset.getWrapper().getAttributeMap();
                if (fileset.getTaskName().equals("tarfileset")) {
                    if (attributes.containsKey("dir")) {
                        String dir = generateToFile(attributeForKey(fileset, "dir"));
                        String predicate = FileSetUtils.getPredicateFromTarFileSet(fileset, this);
                        String prefix = generateToString(attributeForKey(fileset, "prefix"));
                        prefix = prefix.equals("null") ? "\"\"" : prefix;
                        String dirmodeString = getOptionalAttribute(fileset, "dirmode");
                        int dirmode = dirmodeString.equals("") ? DEFAULT_DIR_MODE : Integer.valueOf(dirmodeString);
                        String filemodeString = getOptionalAttribute(fileset, "filemode");
                        int filemode = filemodeString.equals("") ? DEFAULT_FILE_MODE : Integer.valueOf(filemodeString);
                        generator.printString("FileOperations.packageFiles(out, " + dir + ", " + prefix + ", s -> " + predicate + ", "+ dirmode + ", " + filemode + ");");
                    } else if (attributes.containsKey("file") && attributes.containsKey("fullpath") && fileset.getChildren() == null) {
                        // only look at one file
                        String file = generateToFile(attributeForKey(fileset, "file"));
                        String fullpath = generateToString(attributeForKey(fileset, "fullpath"));
                        generator.printString("FileOperations.addFileToArchive(out, " + file + ", "+ fullpath +");");
                    } else {
                        throw new MigrationException("src, dir or file with fullpath inside of tarfileset is not defined");
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

    private String getOptionalAttribute(UnknownElement element, String name) {
        if (this.containsKey(name)) {
            return generateToString(attributeForKey(element, name)).replace("\"","");
        } else return "";
    }

    private int getLongfileCode(String longfileMode) {
        switch(longfileMode) {
            case "truncate": return 1;
            case "fail": return 0;
            case "warn": return 2;
            case "omit": return 0;
            case "gnu": return  2;
            case "posix": return 3;
            // when no mode was specified
            case "": return 2;
            // wrong input
            default : return -1;
        }
    }
}
