package <pkg>.lib;

import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;

public class FileOperations {
    private static final String NULL_PLACEHOLDER = "null";

    //get some non-crypto-grade randomness from various places.
    private static Random rand = new Random(System.currentTimeMillis()
            + Runtime.getRuntime().freeMemory());

    public static void unzip(File src, File dest) {
        Expand expand = new Expand();
        expand.setSrc(src);
        expand.setDest(dest);
        expand.execute();
    }

    public static void copy(File toDir, FileSet fileset) throws IOException {
        FileOperations.copy(toDir, fileset, false);
    }

    public static void copy(File toDir, FileSet fileset, boolean flatten) throws IOException {
        for (Resource resource : fileset) {
            Resource fileResource = resource.as(FileResource.class);

            if (!flatten)
                FileUtils.copyFile(new File(fileset.getDir(), fileResource.getName()), new File(toDir, fileResource.getName()));
            else
                FileUtils.copyFileToDirectory(new File(fileset.getDir(), fileResource.getName()), toDir);
        }
    }

    /**
     * Create a temporary file in a given directory.
     *
     * <p>The file denoted by the returned abstract pathname did not
     * exist before this method was invoked, any subsequent invocation
     * of this method will yield a different file name.</p>
     *
     * @param prefix prefix before the random number.
     * @param suffix file extension; include the '.'.
     * @param parentDir Directory to create the temporary file in;
     * java.io.tmpdir used if not specified.
     * @param deleteOnExit whether to set the tempfile for deletion on
     *        normal VM exit.
     * @param createFile true if the file must actually be created. If false
     * chances exist that a file with the same name is created in the time
     * between invoking this method and the moment the file is actually created.
     * If possible set to true.
     *
     * @return a File reference to the new temporary file.
     */
    public static File createTempFile(String prefix, String suffix, File parentDir,
                               boolean deleteOnExit, boolean createFile) {
        File result = null;
        String parent = (parentDir == null)
                ? System.getProperty("java.io.tmpdir")
                : parentDir.getPath();
        if (prefix == null) {
            prefix = NULL_PLACEHOLDER;
        }
        if (suffix == null) {
            suffix = NULL_PLACEHOLDER;
        }

        if (createFile) {
            try {
                result = File.createTempFile(prefix, suffix, new File(parent));
            } catch (IOException e) {
                throw new BuildException("Could not create tempfile in "
                        + parent, e);
            }
        } else {
            DecimalFormat fmt = new DecimalFormat("#####");
            synchronized (rand) {
                do {
                    result = new File(parent, prefix
                            + fmt.format(rand.nextInt(Integer.MAX_VALUE)) + suffix);
                } while (result.exists());
            }
        }

        if (deleteOnExit) {
            result.deleteOnExit();
        }
        return result;
    }

    public static void downloadFile(URL url, File destination) throws IOException {
        File dest = destination;
        if (destination.isDirectory()) {
            String path = url.getPath();
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            final int slash = path.lastIndexOf("/");
            if (slash > -1) {
                path = path.substring(slash + 1);
            }
            dest = new File(destination, path);
        }

        FileUtils.copyURLToFile(url, dest);
    }
}