package antplutomigrator.generate.transformers;

import antplutomigrator.generate.MigrationException;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.tools.ant.UnknownElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FileSetUtils {
    @NotNull
    public static String getPredicateFromFileSet(UnknownElement fileset, SpecializedTaskTransformer transformer) {
        AntIntrospectionHelper fileSetIntrospectionHelper = AntIntrospectionHelper.getInstanceFor(transformer.elementGenerator.getProject(), fileset, null, transformer.generator.getPkg(), transformer.introspectionHelper);
        if (!fileset.getTaskName().equals("fileset"))
            throw new MigrationException("Copy did contain an unexpected child.");

        if (fileset.getChildren() == null)
            // TODO: Fileset without children does work. Check for file or dir in primitive copy opertaions!
            throw new MigrationException("Fileset need children.");

        List<UnknownElement> includes = new ArrayList<>();
        List<UnknownElement> excludes = new ArrayList<>();
        for (UnknownElement fileSetChild : fileset.getChildren()) {
            if (!fileSetChild.getTaskName().equalsIgnoreCase("exclude"))
                includes.add(fileSetChild);
            else
                excludes.add(fileSetChild);
        }
        String includeTransformed = "";
        if (includes.size() > 0) {
            // handle all includes by putting them in an or
            UnknownElement or = new UnknownElement("or");
            or.setTaskName("or");
            for (UnknownElement include : includes)
                or.addChild(include);

            AntIntrospectionHelper orIntroSpectionHelper = AntIntrospectionHelper.getInstanceFor(transformer.elementGenerator.getProject(), or, null, transformer.generator.getPkg(), fileSetIntrospectionHelper);
            FileSelectorTransformer orTransformer = FileSelectorTransformerFactory.getTransformer(or, transformer.elementGenerator, orIntroSpectionHelper, transformer.generateToFile(fileset.getWrapper().getAttributeMap().get("dir").toString()), "s");
            includeTransformed = orTransformer.transformFileSelector();
        }

        String excludesTransformed = "";
        if (excludes.size() > 0) {
            // handle all excludes by putting them in an and
            UnknownElement or = new UnknownElement("or");
            or.setTaskName("or");
            for (UnknownElement exclude : excludes) {
                // Negation happens outside!
                exclude.setTaskName("include");
                or.addChild(exclude);
            }

            AntIntrospectionHelper andIntroSpectionHelper = AntIntrospectionHelper.getInstanceFor(transformer.elementGenerator.getProject(), or, null, transformer.generator.getPkg(), fileSetIntrospectionHelper);
            FileSelectorTransformer orTransformer = FileSelectorTransformerFactory.getTransformer(or, transformer.elementGenerator, andIntroSpectionHelper, transformer.generateToFile(transformer.attributeForKey(fileset, "dir")), "s");
            excludesTransformed = orTransformer.transformFileSelector();
        }

        String predicateTransformed = "";
        if (!includeTransformed.equals(""))
            predicateTransformed = includeTransformed;
        if (!excludesTransformed.equals("")) {
            if (!predicateTransformed.equals(""))
                predicateTransformed += " && ";
            predicateTransformed += "!" + excludesTransformed;
        }
        return predicateTransformed;
    }
}
