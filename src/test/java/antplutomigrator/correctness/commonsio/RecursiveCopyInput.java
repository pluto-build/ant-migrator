package antplutomigrator.correctness.commonsio;

import build.pluto.dependency.Origin;
import build.pluto.output.Output;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 30.05.17.
 */
public class RecursiveCopyInput implements Serializable, Output {
    private final File sourceDir;
    private final File destDir;
    private final List<String> filter;
    private final Origin origin;

    public File getSourceDir() {
        return sourceDir;
    }

    public File getDestDir() {
        return destDir;
    }

    public List<String> getFilter() {
        return filter;
    }

    public Origin getOrigin() {
        return origin;
    }

    public RecursiveCopyInput(File sourceDir, File destDir) {
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.filter = new ArrayList<>();
        this.origin = null;
    }

    public RecursiveCopyInput(File sourceDir, File destDir, List<String> filter, Origin origin) {
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.filter = filter;
        this.origin = origin;
    }
}
