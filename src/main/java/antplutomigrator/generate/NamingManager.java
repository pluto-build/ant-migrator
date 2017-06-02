package antplutomigrator.generate;

import org.apache.tools.ant.UnknownElement;
import antplutomigrator.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by manuel on 06.02.17.
 */
public class NamingManager {

    private Map<String, Integer> nameMap = new HashMap<>();
    private Map<UnknownElement, String> elementMap = new HashMap<>();

    public String getNameFor(String prefix) {
        prefix = prefix.replace(".", "_").replace("-", "_").trim();
        Integer old = 0;
        if (nameMap.containsKey(prefix)) {
            old = nameMap.get(prefix);
        }

        nameMap.put(prefix, old + 1);
        return prefix + (old + 1);
    }

    public String getNameFor(UnknownElement element) {
        if (!elementMap.containsKey(element)) {
            elementMap.put(element, getNameFor(StringUtils.decapitalize(element.getTaskName())));
        }

        return elementMap.get(element);
    }

    public String getClassNameFor(String name) {
        String replaced = name.replace(".", "_").replace("-", "_").trim();
        while (replaced.indexOf(" ") > -1) {
            replaced = replaced.substring(0, replaced.indexOf(" ")) + replaced.toUpperCase().substring(replaced.indexOf(" ")+1, replaced.indexOf(" ") + 2) + replaced.substring(replaced.indexOf(" ")+2);
        }
        return StringUtils.capitalize(replaced);
    }

}
