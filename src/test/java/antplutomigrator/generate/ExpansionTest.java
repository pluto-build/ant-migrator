package antplutomigrator.generate;

import org.junit.Test;

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
                if (unexpanded.charAt(i+1) == '$') {
                    expanded += "$";
                    i += 2;
                } else
                if (unexpanded.charAt(i+1) == '{') {
                    int start = i+2;
                    int end = unexpanded.indexOf('}', i);
                    if (end < 0)
                        throw new RuntimeException("Property was not closed.");
                    String property = unexpanded.substring(start, end);
                    expanded += get(property);
                    i = end + 1;
                }
            } else {
                expanded += c;
                i++;
            }
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
}
