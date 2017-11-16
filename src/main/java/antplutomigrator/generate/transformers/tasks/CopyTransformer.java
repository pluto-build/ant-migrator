package antplutomigrator.generate.transformers.tasks;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.FileSetUtils;
import antplutomigrator.generate.transformers.SpecializedTaskTransformer;
import org.apache.tools.ant.UnknownElement;

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
        return this.containsOnlySupportedAttributes("file", "tofile", "todir", "overwrite", "flatten", "inputencoding", "outputencoding") && this.supportsChildren(c -> c.getTaskName().equals("fileset") || c.getTaskName().equals("filterset"));
    }

    private boolean isSimpleCopy() {
        return !this.supportsChildren(c -> c.getTaskName().equals("filterset") || c.getTaskName().equals("fileset")) && !containsKey("inputencoding") && !containsKey("outputencoding");
    }

    private String transformReplacements() {
        List<UnknownElement> possibleFilterSets = childrenMatching(c -> c.getTaskName().equals("filterset"));
        if (possibleFilterSets.size() != 1)
            return null;
        List<UnknownElement> children = possibleFilterSets.get(0).getChildren();
        if (children == null || children.size() <= 0)
            return null;

        String replacementsName = namingManager.getNameFor("replacements");
        generator.addImport("java.util.HashMap");
        generator.printString("HashMap<String, String> " + replacementsName + " = new HashMap<>();");

        for (UnknownElement filter : children) {
            // TODO: specialized begin and end
            generator.printString(replacementsName + ".put("+generateToString("@"+attributeForKey(filter, "token")+"@")+", "+generateToString(attributeForKey(filter, "value"))+");");
        }
        return replacementsName;
    }

    @Override
    public void transform() throws RuntimeException {
        if (this.containsKey("file") && this.containsKey("tofile")) {
            if (isSimpleCopy()) {
                //import org.apache.commons.io.FileUtils;
                //FileUtils.copyFile(src, dst);
                generator.addImport("org.apache.commons.io.FileUtils");
                generator.printString("FileUtils.copyFile(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("tofile")) + ");");
            } else {
                String replacementsName = transformReplacements();
                generator.addImport(generator.getPkg() + ".lib.FileOperations");
                generator.printString("FileOperations.copy("+generateToFile(attributeForKey("file"))+", "+generateToFile(attributeForKey("tofile"))+", "+replacementsName+", "+generateToString(attributeForKey("inputencoding"))+", "+generateToString(attributeForKey("outputencoding"))+");");
            }
        }
        if (this.containsKey("file") && this.containsKey("todir")) {
            if (isSimpleCopy()) {
                //import org.apache.commons.io.FileUtils;
                //FileUtils.copyFileToDirectory(src, dst, false);
                generator.addImport("org.apache.commons.io.FileUtils");
                generator.printString("FileUtils.copyFileToDirectory(" + generateToFile(attributeForKey("file")) + ", " + generateToFile(attributeForKey("todir")) + ", false);");
            } else {
                String replacementsName = transformReplacements();
                generator.addImport(generator.getPkg() + ".lib.FileOperations");
                generator.printString("FileOperations.copyToDir("+generateToFile(attributeForKey("file"))+", "+generateToFile(attributeForKey("todir"))+", "+replacementsName+", "+generateToString(attributeForKey("inputencoding"))+", "+generateToString(attributeForKey("outputencoding"))+");");
            }
        }
        if (this.element.getChildren() != null && this.containsKey("todir")) {
            for (UnknownElement fileset : this.element.getChildren()) {
                String predicateTransformed = FileSetUtils.getPredicateFromFileSet(fileset, this);

                String flatten = null;
                if (containsKey("flatten"))
                    flatten = generateToBoolean(attributeForKey("flatten"));

                generator.addImport(generator.getPkg() + ".lib.FileOperations");
                generator.printString("FileOperations.copy(" + generateToFile(this.attributeForKey(fileset, "dir")) + ", " + generateToFile(this.attributeForKey("todir")) + ", s -> " + predicateTransformed + ((flatten != null) ? (", " + flatten + ", true") : "") + ");");
            }
        }
    }

}
