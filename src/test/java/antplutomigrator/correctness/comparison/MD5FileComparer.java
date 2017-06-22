package antplutomigrator.correctness.comparison;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by manuel on 22.06.17.
 */
public class MD5FileComparer implements FileComparer {
    @Override
    public boolean filesAreEqual(File f1, File f2) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f1);
            FileInputStream fis2 = new FileInputStream(f2);
            String md5 = DigestUtils.md5Hex(fis);
            String md52 = DigestUtils.md5Hex(fis2);
            fis.close();
            fis2.close();
            return md5.equals(md52);
        } catch (Exception e) {
            return false;
        }
    }
}
