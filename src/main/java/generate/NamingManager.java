package generate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by manuel on 06.02.17.
 */
public class NamingManager {

    private Map<String, Integer> nameMap = new HashMap<>();

    public String getNameFor(String prefix) {
        Integer old = 0;
        if (nameMap.containsKey(prefix)) {
            old = nameMap.get(prefix);
        }

        nameMap.put(prefix, old + 1);
        return prefix + (old + 1);
    }

}
