package antplutomigrator.correctness.comparison;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by manuel on 02.06.17.
 */
public class DirectoryComparer {
    private final List<File> dirs;

    public List<File> getDirs() {
        return dirs;
    }

    public DirectoryComparer(List<File> dirs) {
        this.dirs = dirs;
        for (File d: dirs) {
            assert d.isDirectory();
        }
    }

    public void compare(FileMismatch fileMismatch) throws ComparisonException, IOException {
        for (File file: dirs) {
            for (File file2: dirs) {
                if (file != file2) {
                    compare(file, file2, fileMismatch);
                }
            }
        }
    }

    public void compare(File f1, File f2, FileMismatch fileMismatch) throws ComparisonException, IOException {
        if (f1.isDirectory()) {
            assert f2.isDirectory();

            for (File c: f1.listFiles()) {
                File[] c2s = f2.listFiles((f,  n) -> n.equals(c.getName()));
                if (c2s == null || c2s.length == 0)
                    throw new ComparisonException("Could not find file " + c.getName() + " in " + f2.getAbsolutePath(), f1, f2);
                compare(c, c2s[0], fileMismatch);
            }
        } else {
            assert !f2.isDirectory();

            FileInputStream fis = new FileInputStream(f1);
            FileInputStream fis2 = new FileInputStream(f2);
            String md5 = DigestUtils.md5Hex(fis);
            String md52 = DigestUtils.md5Hex(fis2);
            fis.close();
            fis2.close();
            if (!md5.equals(md52)) {
                try {
                    if (!fileMismatch.mismatchFound(f1, f2)) {
                        throw new ComparisonException("Mismatch found between " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + " which couldn't be resolved.", f1, f2);
                    }
                } catch (Exception e) {
                    throw new ComparisonException("Mismatch found between " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + " which couldn't be resolved. (Exception " + e.getMessage() + ")", f1, f2);
                }
            }
        }
    }
}
