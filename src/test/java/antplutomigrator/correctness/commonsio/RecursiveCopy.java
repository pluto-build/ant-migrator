package antplutomigrator.correctness.commonsio;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by manuel on 30.05.17.
 */
public class RecursiveCopy extends Builder<RecursiveCopyInput, RecursiveCopyInput> {
    public static BuilderFactory<RecursiveCopyInput, RecursiveCopyInput, RecursiveCopy> factory = BuilderFactoryFactory.of(RecursiveCopy.class, RecursiveCopyInput.class);

    public RecursiveCopy(RecursiveCopyInput input) {
        super(input);
    }

    @Override
    protected String description(RecursiveCopyInput recursiveCopyInput) {
        return "Copying " + recursiveCopyInput.getSourceDir().getPath() + " to " + recursiveCopyInput.getDestDir().getPath();
    }

    @Override
    public File persistentPath(RecursiveCopyInput recursiveCopyInput) {
        return new File(recursiveCopyInput.getDestDir(), ".deps/copy.dep");
    }

    @Override
    protected RecursiveCopyInput build(RecursiveCopyInput recursiveCopyInput) throws Throwable {
        assert(recursiveCopyInput.getSourceDir().isDirectory());

        if (recursiveCopyInput.getOrigin() != null)
            requireBuild(recursiveCopyInput.getOrigin());

        File[] files = recursiveCopyInput.getSourceDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (recursiveCopyInput.getFilter() == null)
                    return true;

                return !recursiveCopyInput.getFilter().contains(pathname.getName());
            }
        });

        for (File f: files) {
            File d = new File(recursiveCopyInput.getDestDir(), f.getName());
            if (f.isDirectory()) {
                requireBuild(RecursiveCopy.factory, new RecursiveCopyInput(f, d, recursiveCopyInput.getFilter(), recursiveCopyInput.getOrigin()));
            } else {
                require(f);
                FileUtils.copyFile(f, d);
                provide(d);
            }
        }

        return recursiveCopyInput;
    }
}
