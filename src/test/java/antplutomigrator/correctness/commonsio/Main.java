package antplutomigrator.correctness.commonsio;

import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.output.None;

/**
 * Created by manuel on 29.05.17.
 */
public class Main {
    public static void main(String[] args) throws Throwable {
        BuildManagers.build(new BuildRequest<>(PrepareDownloads.factory, None.val));
    }
}
