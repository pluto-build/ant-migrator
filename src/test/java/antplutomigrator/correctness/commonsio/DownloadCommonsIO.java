package antplutomigrator.correctness.commonsio;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.GitRemoteSynchronizer;
import build.pluto.buildgit.bound.TagBound;
import build.pluto.output.None;

import java.io.File;

/**
 * Created by manuel on 29.05.17.
 */
public class DownloadCommonsIO extends Builder<None, None> {

    public static BuilderFactory<None, None, DownloadCommonsIO> factory = BuilderFactoryFactory.of(DownloadCommonsIO.class, None.class);

    public DownloadCommonsIO(None input) {
        super(input);
    }

    @Override
    protected String description(None none) {
        return "Downloading Commons IO";
    }

    @Override
    public File persistentPath(None none) {
        return new File("deps/download_commons_io.dep");
    }

    @Override
    protected None build(None none) throws Throwable {

        GitInput gitInput = new GitInput.Builder(new File("testdata/antplutomigrator.correctness/commonsio/source"), "https://github.com/apache/commons-io.git")
                .setBound(new TagBound("master", "commons-io-2.5")).build();

        requireBuild(GitRemoteSynchronizer.factory, gitInput);

        return None.val;
    }
}
