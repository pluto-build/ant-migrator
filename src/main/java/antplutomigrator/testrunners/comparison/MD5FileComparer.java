package antplutomigrator.testrunners.comparison;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by manuel on 22.06.17.
 */
public class MD5FileComparer implements FileComparer {
    @Override
    public boolean filesAreEqual(File f1, File f2) {
        try {
            byte[] b1 = FileUtils.readFileToByteArray(f1);
            byte[] b2 = FileUtils.readFileToByteArray(f2);

            if (b1.length != b2.length)
                return false;

            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i])
                    return false;
            }
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
