package antplutomigrator.correctness;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class PropertyLoader {
    public static Properties getProperties(String name) throws IOException {
        Properties prop = new Properties();
        URL resource = PropertyLoader.class.getResource(name);
        InputStream input = new FileInputStream(resource.getFile());

        // load a properties file
        prop.load(input);

        return prop;
    }
}
