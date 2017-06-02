package antplutomigrator.correctness.comparison;

import java.io.File;

/**
 * Created by manuel on 02.06.17.
 */
public class ComparisonException extends Exception {
    private final File file1;
    private final File file2;

    public ComparisonException(String message, File file1, File file2) {
        super(message);
        this.file1 = file1;
        this.file2 = file2;
    }
}
