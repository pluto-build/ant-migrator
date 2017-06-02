package antplutomigrator.correctness.comparison;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;

/**
 * Created by manuel on 02.06.17.
 */
public interface FileMismatch {
    boolean mismatchFound(File f1, File f2) throws Exception;
}
