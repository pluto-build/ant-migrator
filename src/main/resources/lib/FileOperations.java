package <pkg>.lib;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class FileOperations {
    public static void unzip(File src, File dest) {
        Expand expand = new Expand();
        expand.setSrc(src);
        expand.setDest(dest);
        expand.execute();
    }

    public static void copy(File toDir, FileSet fileset) throws IOException {
        for (Resource resource : fileset) {
            Resource fileResource = resource.as(FileResource.class);

            FileUtils.copyFile(new File(fileset.getDir(), fileResource.getName()), new File(toDir, fileResource.getName()));
        }
    }
}