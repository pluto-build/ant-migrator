package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by manuel on 08.06.17.
 */
public class MD5CheckTask extends TestTask {

    private final File file;
    private final String md5Hex;

    public MD5CheckTask(File file, String md5Hex) {
        this.file = file;
        this.md5Hex = md5Hex;
    }

    @Override
    public String getDescription() {
        return "Checking md5 hash of " + file;
    }

    @Override
    public void execute() throws Exception {
        assertEquals(md5Hex, DigestUtils.md5Hex(new FileInputStream(file)));
    }
}
