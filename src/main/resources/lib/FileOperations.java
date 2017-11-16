package <pkg>.lib;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.LineTokenizer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class FileOperations {
    private static final String NULL_PLACEHOLDER = "null";

    //get some non-crypto-grade randomness from various places.
    private static Random rand = new Random(System.currentTimeMillis()
            + Runtime.getRuntime().freeMemory());

    public static void unzip(File src, File dest) throws IOException {
        ZipFile zipFile = new ZipFile(src);
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        for (ZipArchiveEntry entry : Collections.list(entries)) {
            File destFile = new File(dest, entry.getName());
            if (entry.isDirectory())
                destFile.mkdirs();
            else {
                // copy file
                destFile.getParentFile().mkdirs();
                InputStream in = zipFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(destFile);
                IOUtils.copy(in, out);
                in.close();
                out.close();
            }
        }
        zipFile.close();
    }

    public static void gunzip(File src, File dest) throws IOException {
        // only expand if dest does not exists or src is newer than dest
        if (dest.exists() && src.lastModified() <= dest.lastModified()) {
            return;
        }
        GzipCompressorInputStream in = new GzipCompressorInputStream(new FileInputStream(src));
        src.createNewFile();
        OutputStream out = new FileOutputStream(dest);
        IOUtils.copy(in, out);
        in.close();
        out.close();
    }

    public static void untar(File src, File dest) throws IOException {
        TarArchiveInputStream tar = new TarArchiveInputStream(new FileInputStream(src));
        TarArchiveEntry entry = tar.getNextTarEntry();
        while (entry != null) {
            File destEntry = new File (dest, entry.getName());
            if (entry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                // copy file
                destEntry.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(destEntry.getAbsoluteFile());
                IOUtils.copy(tar, out);
                out.close();
            }
            entry = tar.getNextTarEntry();
        }
        tar.close();
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

    public static final String[] DEFAULTEXCLUDES = { //NOSONAR
            // Miscellaneous typical temporary files
            SelectorUtils.DEEP_TREE_MATCH + "/*~",
            SelectorUtils.DEEP_TREE_MATCH + "/#*#",
            SelectorUtils.DEEP_TREE_MATCH + "/.#*",
            SelectorUtils.DEEP_TREE_MATCH + "/%*%",
            SelectorUtils.DEEP_TREE_MATCH + "/._*",

            // CVS
            SelectorUtils.DEEP_TREE_MATCH + "/CVS",
            SelectorUtils.DEEP_TREE_MATCH + "/CVS/" + SelectorUtils.DEEP_TREE_MATCH,
            SelectorUtils.DEEP_TREE_MATCH + "/.cvsignore",

            // SCCS
            SelectorUtils.DEEP_TREE_MATCH + "/SCCS",
            SelectorUtils.DEEP_TREE_MATCH + "/SCCS/" + SelectorUtils.DEEP_TREE_MATCH,

            // Visual SourceSafe
            SelectorUtils.DEEP_TREE_MATCH + "/vssver.scc",

            // Subversion
            SelectorUtils.DEEP_TREE_MATCH + "/.svn",
            SelectorUtils.DEEP_TREE_MATCH + "/.svn/" + SelectorUtils.DEEP_TREE_MATCH,

            // Git
            SelectorUtils.DEEP_TREE_MATCH + "/.git",
            SelectorUtils.DEEP_TREE_MATCH + "/.git/" + SelectorUtils.DEEP_TREE_MATCH,
            SelectorUtils.DEEP_TREE_MATCH + "/.gitattributes",
            SelectorUtils.DEEP_TREE_MATCH + "/.gitignore",
            SelectorUtils.DEEP_TREE_MATCH + "/.gitmodules",

            // Mercurial
            SelectorUtils.DEEP_TREE_MATCH + "/.hg",
            SelectorUtils.DEEP_TREE_MATCH + "/.hg/" + SelectorUtils.DEEP_TREE_MATCH,
            SelectorUtils.DEEP_TREE_MATCH + "/.hgignore",
            SelectorUtils.DEEP_TREE_MATCH + "/.hgsub",
            SelectorUtils.DEEP_TREE_MATCH + "/.hgsubstate",
            SelectorUtils.DEEP_TREE_MATCH + "/.hgtags",

            // Bazaar
            SelectorUtils.DEEP_TREE_MATCH + "/.bzr",
            SelectorUtils.DEEP_TREE_MATCH + "/.bzr/" + SelectorUtils.DEEP_TREE_MATCH,
            SelectorUtils.DEEP_TREE_MATCH + "/.bzrignore",

            // Mac
            SelectorUtils.DEEP_TREE_MATCH + "/.DS_Store"
    };

    public static List<String> matchedFiles(File baseDir, Predicate<String> includeFilePredicate) {
        return matchedFiles(baseDir, includeFilePredicate, true);
    }

    public static List<String> matchedFiles(File baseDir, Predicate<String> includeFilePredicate, boolean useDefaultExcludes) {
        if (useDefaultExcludes)
            return matchedFiles(baseDir, "", s -> {
                for (String exclude: DEFAULTEXCLUDES) {
                    if (SelectorUtils.matchPath(exclude, s)) {
                        return false;
                    }
                }
                return includeFilePredicate.test(s);
            });
        return matchedFiles(baseDir, "", includeFilePredicate);
    }

    private static List<String> matchedFiles(File baseDir, String previousString, Predicate<String> includeFilePredicate) {
        System.out.println(" Checking files in " + baseDir.getAbsolutePath());
        ArrayList<String> files = new ArrayList<>();
        String[] newfileNames = new File(baseDir, previousString).list();
        for (String newFileName: newfileNames) {
            String newFileString = previousString +"/" + newFileName;
            if (newFileString.startsWith("/"))
                newFileString = newFileString.substring(1);
            System.out.println(" Checking " + newFileString);
            File newFile = new File(baseDir, newFileString);
            if (includeFilePredicate.test(newFileString)) {
                System.out.println("  -> included");
                files.add(newFileString);
            }
            if (newFile.exists() && newFile.isDirectory())
                files.addAll(matchedFiles(baseDir, previousString + "/" + newFileName, includeFilePredicate));
        }
        return files;
    }

    public static void copy(File baseDir, File toDir, Predicate<String> includeFilePredicate) throws IOException {
        FileOperations.copy(baseDir, toDir, includeFilePredicate, false, true);
    }

    public static void copy(File baseDir, File toDir, Predicate<String> includeFilePredicate, boolean flatten, boolean useDefaultExcludes) throws IOException {
        System.out.println("Copying files from " + baseDir.getAbsolutePath() +" to "+ toDir.getAbsolutePath());
        for (String fileString : matchedFiles(baseDir, includeFilePredicate, useDefaultExcludes)) {
            System.out.println("  Copying " + fileString + " from " + baseDir.getAbsolutePath() +" to "+ toDir.getAbsolutePath());
            File srcFile = new File(baseDir, fileString);
            File destFile = new File(toDir, fileString);
            if (!flatten) {
                if (srcFile.isFile())
                    FileUtils.copyFile(srcFile, destFile);
                //else
                //    FileUtils.copyDirectoryToDirectory(srcFile, destFile.getParentFile());
            }
            else {
                if (srcFile.isFile())
                    FileUtils.copyFileToDirectory(srcFile, toDir);
                // TODO: Is this correct?
                //else
                //    FileUtils.copyDirectoryToDirectory(srcFile, toDir);
            }
        }
    }

    public static void copyToDir(File from, File toDir, HashMap<String, String> replacements, String inputEncoding, String outputEncoding) throws IOException {
        FileOperations.copy(from, new File(toDir, from.getName()), replacements, inputEncoding, outputEncoding);
    }

    public static void copy(File from, File to, HashMap<String, String> replacements, String inputEncoding, String outputEncoding) throws IOException {
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            InputStreamReader isr = null;
            if (inputEncoding == null) {
                isr = new InputStreamReader(new FileInputStream(from));
            } else {
                isr = new InputStreamReader(new FileInputStream(from),
                        inputEncoding);
            }
            in = new BufferedReader(isr);
            final OutputStream os = new FileOutputStream(to);
            OutputStreamWriter osw;
            if (outputEncoding == null) {
                osw = new OutputStreamWriter(os);
            } else {
                osw = new OutputStreamWriter(os, outputEncoding);
            }
            out = new BufferedWriter(osw);

            final LineTokenizer lineTokenizer = new LineTokenizer();
            lineTokenizer.setIncludeDelims(true);
            String newline = null;
            String line = lineTokenizer.getToken(in);
            while (line != null) {
                if (line.length() == 0) {
                    // this should not happen, because the lines are
                    // returned with the end of line delimiter
                    out.newLine();
                } else {
                    newline = line;
                    if (replacements != null)
                        for (Map.Entry<String, String> entry : replacements.entrySet()) {
                            newline = newline.replace(entry.getKey(), entry.getValue());
                        }
                    out.write(newline);
                }
                line = lineTokenizer.getToken(in);
            }
        } finally {
            out.close();
            in.close();
        }
    }

    public static void copy(File baseDir, File toDir, Predicate<String> includeFilePredicate, HashMap<String, String> replacements, String inputEncoding, String outputEncoding, boolean flatten, boolean useDefaultExcludes) throws IOException {
        if ((replacements == null || replacements.isEmpty()) && inputEncoding == null && outputEncoding == null) {
            FileOperations.copy(baseDir, toDir, includeFilePredicate, flatten, useDefaultExcludes);
            return;
        }

        for (String fileString : matchedFiles(baseDir, includeFilePredicate, useDefaultExcludes)) {
            File source = new File(baseDir, fileString);
            File destination = toDir;
            if (!flatten)
                destination = new File(toDir, fileString);

            FileOperations.copy(source, destination, replacements, inputEncoding, outputEncoding);
        }
    }

    public static void copy(File baseDir, File toDir, Predicate<String> includeFilePredicate, HashMap<String, String> replacements) throws IOException {
        copy(baseDir, toDir, includeFilePredicate, replacements, null, null, false, true);
    }
}
