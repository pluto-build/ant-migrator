package antplutomigrator.generate.antbehavior;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SelectorUtilsTest {

    @Test
    public void testMatchPath1() {
        assertEquals(false, SelectorUtils.matchPath("docs/architecture/*.xml", "docs/architecture/build.xml.txt"));
    }
}
