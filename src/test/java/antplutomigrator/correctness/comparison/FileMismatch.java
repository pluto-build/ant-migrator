package antplutomigrator.correctness.comparison;

import java.io.File;

/**
 * Created by manuel on 02.06.17.
 */
public interface FileMismatch {
    boolean mismatchFound(File f1, File f2) throws Exception;
}
