package antplutomigrator.generate;

import org.apache.tools.ant.UnknownElement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by manuel on 09.02.17.
 */
public class NamingManagerTest {

    NamingManager namingManager;

    @Before
    public void setup() {
        namingManager = new NamingManager();
    }

    @Test
    public void testUniqueness() {
        String name1 = namingManager.getNameFor("test");
        String name2 = namingManager.getNameFor("test");
        assertNotEquals(name1, name2);
    }

    @Test
    public void testNameForUnknownElement() {
        UnknownElement e1 = new UnknownElement("e");
        e1.setTaskName("e");
        UnknownElement e2 = new UnknownElement("e");
        e2.setTaskName("e");

        String n1 = namingManager.getNameFor(e1);
        assertEquals(namingManager.getNameFor(e1), n1);
        String n2 = namingManager.getNameFor(e2);
        assertEquals(namingManager.getNameFor(e2), n2);

        assertNotEquals(n1, n2);
    }

    @Test
    public void testRenamingDot() {
        assertEquals("Some_test", namingManager.getClassNameFor("some.test"));
    }

    @Test
    public void testRenamingSpace1() {
        assertEquals("SomeTest", namingManager.getClassNameFor("some test"));
    }

    @Test
    public void testRenamingSpace2() {
        assertEquals("SomeMoreTests", namingManager.getClassNameFor(" some more tests "));
    }
}
