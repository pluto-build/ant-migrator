package antplutomigrator.generate.antbehavior;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertTrue;

public class FileOperationsTest {

    public boolean predicate(String fileName, Predicate<String> predicate) {
        return predicate.test(fileName);
    }

    @Test
    public void testPredicate1() {
        assertTrue(predicate("docs/appdev/build.xml.txt", s ->
                (SelectorUtils.matchPath("docs/images/**", s)
                        || SelectorUtils.matchPath("docs/WEB-INF/**", s)
                        || SelectorUtils.matchPath("docs/appdev/*.txt", s)
                        || SelectorUtils.matchPath("docs/appdev/sample/**", s))));
    }
}
