package antplutomigrator.correctness.comparison;

import org.apache.commons.io.FileUtils;

import javax.sound.sampled.Line;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 22.06.17.
 */
public class LineByLineFileComparer implements FileComparer {
    private final List<LineComparer> lineComparers;

    public LineByLineFileComparer() {
        this.lineComparers = new ArrayList<>();

        this.addLineComparer(new EqualLineComparer());
    }

    public LineByLineFileComparer(List<LineComparer> lineComparers) {
        this.lineComparers = lineComparers;
    }

    public void addLineComparer(LineComparer lineComparer) {
        this.lineComparers.add(lineComparer);
    }

    @Override
    public boolean filesAreEqual(File f1, File f2) {
        try {
            List<String> f1Lines = FileUtils.readLines(f1);
            List<String> f2Lines = FileUtils.readLines(f1);
            if (f1Lines.size() != f2Lines.size())
                return false;

            for (int i = 0; i < f1Lines.size(); i++) {
                String f1Line = f1Lines.get(i);
                String f2Line = f2Lines.get(i);

                boolean linesEqual = false;
                for (LineComparer lineComparer: lineComparers) {
                    if (lineComparer.linesAreEqual(f1, f2, f1Line, f2Line)) {
                        linesEqual = true;
                        break;
                    }
                }
                if (!linesEqual)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
