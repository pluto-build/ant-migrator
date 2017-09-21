package antplutomigrator.generate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpansionTest {
    private String get(String key) {
        return "<"+key+">";
    }

    public String expand(String unexpanded) {
        int i = 0;
        String expanded = "";
        while (i < unexpanded.length()) {
            char c = unexpanded.charAt(i);
            if (c == '$') {
                if (i+1 < unexpanded.length()) {
                    if (unexpanded.charAt(i + 1) == '$') {
                        i++;
                    } else if (unexpanded.charAt(i + 1) == '{') {
                        int start = i + 2;
                        int end = unexpanded.indexOf('}', i);
                        if (end >= 0) {
                            String property = unexpanded.substring(start, end);
                            expanded += get(property);
                            i = end + 1;
                            continue;
                        }
                    }
                }
            }
            expanded += c;
            i++;
        }

        return expanded;
    }

    @Test
    public void testNormalExpansion1() {
        assert expand("This ${first} is ${second} a test.").equals("This <first> is <second> a test.");
    }

    @Test
    public void testDoubleDollar1() {
        assert expand("This string contains a $$.").equals("This string contains a $.");
    }

    @Test
    public void testDoubleDollar2() {
        assert expand("This $${doesnt} expand").equals("This ${doesnt} expand");
    }


    @Test
    public void testDoubleDollar3() {
        assert expand("$$$$").equals("$$");
    }

    @Test
    public void testDoubleDollar4() {
        assert expand("$$$").equals("$$");
    }

    @Test
    public void testDoubleDollar5() {
        assertEquals("${", expand("${"));
    }

    @Test
    public void testDoubleDollar6() {
        assert expand("$").equals("$");
    }

    @Test
    public void testTomcatExample1() {
        assert expand("**/javax.websocket.server.ServerEndpointConfig$Configurator").equals("**/javax.websocket.server.ServerEndpointConfig$Configurator");
    }
}
