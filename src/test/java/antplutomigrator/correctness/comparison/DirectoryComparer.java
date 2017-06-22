package antplutomigrator.correctness.comparison;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 02.06.17.
 */
public class DirectoryComparer {
    private final List<FileComparer> fileComparers;

    public List<FileComparer> getFileComparers() {
        return fileComparers;
    }

    public void addFileComparer(FileComparer fileComparer) {
        this.fileComparers.add(fileComparer);
    }

    public DirectoryComparer() {
        fileComparers = new ArrayList<>();
        this.addFileComparer(new MD5FileComparer());
    }

    public DirectoryComparer(List<FileComparer> fileComparers) {
        this.fileComparers = fileComparers;
    }

    public void compare(List<File> dirs) throws ComparisonException, IOException {
        for (File file : dirs) {
            for (File file2 : dirs) {
                if (file != file2) {
                    compare(file, file2);
                }
            }
        }
    }

    public void compare(File f1, File f2) throws ComparisonException, IOException {
        if (f1.isDirectory()) {
            assert f2.isDirectory();

            for (File c : f1.listFiles()) {
                File[] c2s = f2.listFiles((f, n) -> n.equals(c.getName()));
                if (c2s == null || c2s.length == 0)
                    throw new ComparisonException("Could not find file " + c.getName() + " in " + f2.getAbsolutePath(), f1, f2);
                compare(c, c2s[0]);
            }
        } else {
            assert !f2.isDirectory();

            boolean comparisonResult = false;
            for (FileComparer fileComparer : fileComparers) {
                if (fileComparer.filesAreEqual(f1, f2)) {
                    comparisonResult = true;
                    break;
                }
            }
            if (!comparisonResult)
                throw new ComparisonException("Mismatch found between " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + " which couldn't be resolved.", f1, f2);
        }
    }
}
