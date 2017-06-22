package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.comparison.FileComparer;
import antplutomigrator.correctness.comparison.LineComparer;
import antplutomigrator.correctness.utils.TestTask;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;

/**
 * Created by manuel on 21.06.17.
 */
public class LineByLineComparerTask extends TestTask {
    private final File src1;
    private final File src2;
    private final FileComparer fileComparer;
    private final LineComparer lineComparer;

    public LineByLineComparerTask(File src1, File src2, LineComparer lineComparer, FileComparer fileComparer) {
        this.src1 = src1;
        this.src2 = src2;
        this.lineComparer = lineComparer;
        this.fileComparer = fileComparer;
    }

    @Override
    public String getDescription() {
        return "Comparing " + src1 + " and " + src2;
    }

    @Override
    public void execute() throws Exception {
        // TODO: Implement this
        throw new NotImplementedException();
        /*DeepUnzipDirectoryComparer directoryComparer = new DeepUnzipDirectoryComparer(Arrays.asList(src1, src2));
        directoryComparer.compare((f1, f2) -> {
            if (fileComparer.filesAreEqual(f1, f2))
                return true;
            List<String> f1Lines = FileUtils.readLines(f1);
            List<String> f2Lines = FileUtils.readLines(f1);
            if (f1Lines.size() != f2Lines.size())
                return false;

            for (int i = 0; i < f1Lines.size(); i++) {
                String f1Line = f1Lines.get(i);
                String f2Line = f2Lines.get(i);

                if (!f1Line.equals(f2Line)) {
                    if (!lineComparer.linesAreEqual(f1, f2, f1Line, f2Line))
                        return false;
                }
            }

            return false;
        });*/
    }
}
