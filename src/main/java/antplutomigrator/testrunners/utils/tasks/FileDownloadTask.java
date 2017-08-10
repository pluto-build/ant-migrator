package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

/**
 * Created by manuel on 08.06.17.
 */
public class FileDownloadTask extends TestTask {
    private final URL url;
    private final File destination;

    public FileDownloadTask(URL url, File destination) {
        this.url = url;
        this.destination = destination;
    }

    @Override
    public String getDescription() {
        return "Downloading " + url + " to " + destination;
    }

    @Override
    public void execute() throws Exception {
        FileUtils.copyURLToFile(url, destination);
    }
}
