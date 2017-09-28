package <pkg>.lib;

import org.apache.tools.ant.taskdefs.Expand;

import java.io.File;

public class FileOperations {
    public static void unzip(File src, File dest) {
        Expand expand = new Expand();
        expand.setSrc(src);
        expand.setDest(dest);
        expand.execute();
    }
}