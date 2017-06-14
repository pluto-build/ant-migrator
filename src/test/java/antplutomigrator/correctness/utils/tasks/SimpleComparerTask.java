package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.comparison.ComparisonException;
import antplutomigrator.correctness.comparison.DirectoryComparer;
import antplutomigrator.correctness.utils.TestTask;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;

/**
 * Created by manuel on 08.06.17.
 */
public class SimpleComparerTask extends TestTask {
    private final File src1;
    private final File src2;

    public SimpleComparerTask(File src1, File src2) {
        this.src1 = src1;
        this.src2 = src2;
    }

    @Override
    public String getDescription() {
        return "Comparing " + src1 + " and " + src2;
    }

    @Override
    public void execute() throws Exception {
        DirectoryComparer directoryComparer = new DirectoryComparer(Arrays.asList(src1, src2));
        directoryComparer.compare(((f1, f2) -> {
            if (f1.getName().endsWith("jar")) {
                System.err.println("Found differently hashed jars. Doing deep diff. ("+ f1 + " ≠ " + f2 + ")");
                File tempDir = FileUtils.getTempDirectory();
                File f1Tmp = new File(tempDir, "f1");
                File f2Tmp = new File(tempDir, "f2");
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

                DirectoryComparer directoryComparer1 = new DirectoryComparer(Arrays.asList(f1Tmp, f2Tmp));
                try {
                    // We don't supported nested jar differences here...
                    directoryComparer1.compare((f11, f21) -> false);
                } catch (ComparisonException e) {
                    return false;
                } finally {
                    FileUtils.deleteDirectory(f1Tmp);
                    FileUtils.deleteDirectory(f2Tmp);
                }
                return true;
            }
            return false;
        }
        ));
    }
}
