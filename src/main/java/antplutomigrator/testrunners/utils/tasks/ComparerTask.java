package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.comparison.DirectoryComparer;
import antplutomigrator.testrunners.comparison.FileComparer;
import antplutomigrator.testrunners.utils.TestTask;

import java.io.File;
import java.util.Arrays;

/**
 * Created by manuel on 08.06.17.
 */
public class ComparerTask extends TestTask {
    private final File src1;
    private final File src2;
    private final DirectoryComparer directoryComparer;

    public DirectoryComparer getDirectoryComparer() {
        return directoryComparer;
    }

    public File getSrc1() {
        return src1;
    }

    public File getSrc2() {
        return src2;
    }

    public ComparerTask(File src1, File src2) {
        this.src1 = src1;
        this.src2 = src2;
        this.directoryComparer = new DirectoryComparer();
    }

    @Override
    public String getDescription() {
        return "Comparing " + src1 + " and " + src2;
    }

    @Override
    public void execute() throws Exception {
        directoryComparer.compare(Arrays.asList(src1, src2));
    }
}
