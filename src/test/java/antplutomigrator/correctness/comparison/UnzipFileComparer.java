package antplutomigrator.correctness.comparison;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;

/**
 * Created by manuel on 22.06.17.
 */
public class UnzipFileComparer implements FileComparer {

    private final DirectoryComparer baseComparer;

    public UnzipFileComparer(DirectoryComparer baseComparer) {
        this.baseComparer = baseComparer;
    }

    @Override
    public boolean filesAreEqual(File f1, File f2) {
        if (f1.getName().endsWith("jar") || f1.getName().endsWith("zip")) {
            try {
                File tempDir = FileUtils.getTempDirectory();
                File f1Tmp = new File(tempDir, "antplutomigrator/"+f1.getName().replace(".","_"));
                File f2Tmp = new File(tempDir, "antplutomigrator/"+f2.getName().replace(".","_"));
                ZipFile f1z = new ZipFile(f1);
                f1z.setRunInThread(true);
                f1z.extractAll(f1Tmp.getAbsolutePath());
                while (f1z.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
                    Thread.sleep(10);
                ZipFile f2z = new ZipFile(f2);
                f2z.setRunInThread(true);
                f2z.extractAll(f2Tmp.getAbsolutePath());
                while (f2z.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
                    Thread.sleep(10);

                try {
                    baseComparer.compare(Arrays.asList(f1Tmp, f2Tmp));
                } catch (ComparisonException e) {
                    return false;
                } finally {
                    FileUtils.deleteDirectory(f1Tmp);
                    FileUtils.deleteDirectory(f2Tmp);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
