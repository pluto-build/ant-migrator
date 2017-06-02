package antplutomigrator.correctness.commonsio;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.dependency.Origin;
import build.pluto.output.None;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;

/**
 * Created by manuel on 29.05.17.
 */
public class PrepareDownloads extends Builder<None, None> {

    public static BuilderFactory<None, None, PrepareDownloads> factory = BuilderFactoryFactory.of(PrepareDownloads.class, None.class);

    public PrepareDownloads(None input) {
        super(input);
    }

    @Override
    protected String description(None none) {
        return "Preparing downloaded git repostitory.";
    }

    @Override
    public File persistentPath(None none) {
        return new File("deps/prepare_downloads.dep");
    }

    @Override
    protected None build(None none) throws Throwable {
        requireBuild(DownloadCommonsIO.factory, None.val);

        Origin downloadOrigin = Origin.from(lastBuildReq());

        File source = new File("testdata/antplutomigrator.correctness/commonsio/source");
        File destAnt = new File("testdata/antplutomigrator.correctness/commonsio/ant");
        File destPluto = new File("testdata/antplutomigrator.correctness/commonsio/pluto");

        requireBuild(RecursiveCopy.factory, new RecursiveCopyInput(source, destAnt, Arrays.asList(".git", "build.xml"), downloadOrigin));
        requireBuild(RecursiveCopy.factory, new RecursiveCopyInput(source, destPluto, Arrays.asList(".git", "build.xml"), downloadOrigin));

        File buildFileSrc = new File(source, "build.xml");
        File buildFileDstAnt = new File(destAnt,"build.xml");
        File buildFileDstPluto = new File(destPluto,"build.xml");

        require(buildFileSrc);

        String buildFileStr = FileUtils.readFileToString(buildFileSrc);
        buildFileStr = buildFileStr.replaceAll("https://hamcrest\\.googlecode\\.com/files/hamcrest-core-1\\.3\\.jar", "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/hamcrest/hamcrest-core-1.3.jar");

        FileUtils.writeStringToFile(buildFileDstAnt, buildFileStr);
        FileUtils.writeStringToFile(buildFileDstPluto, buildFileStr);

        provide(buildFileDstAnt);
        provide(buildFileDstPluto);

        return None.val;
    }
}
