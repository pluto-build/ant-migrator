package antplutomigrator.correctness.utils.tasks;

import antplutomigrator.correctness.utils.TestTask;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class ProvideDownloadTask extends TestTask {

    private URL url;
    private String md5;
    private File destFile;

    public ProvideDownloadTask(URL url, String md5, File destFile) {
        this.url = url;
        this.md5 = md5;
        this.destFile = destFile;
    }

    @Override
    public String getDescription() {
        return "Providing " + url.toString() + " to " + destFile;
    }

    @Override
    public void execute() throws Exception {
        if (destFile.exists()) {
            if (!DigestUtils.md5Hex(new FileInputStream(destFile)).equals(md5))
                destFile.delete();
            else
                return;
        }

        new FileDownloadTask(url, destFile).execute();

        assert destFile.exists();
        assert DigestUtils.md5Hex(new FileInputStream(destFile)).equals(md5);
    }
}
