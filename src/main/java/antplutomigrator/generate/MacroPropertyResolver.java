package antplutomigrator.generate;

import antplutomigrator.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 21.02.17.
 */
public class MacroPropertyResolver implements Resolvable {

    private final Resolvable baseResolver;
    private List<String> attributes = new ArrayList<>();
    private NamingManager namingManager = new NamingManager();

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }


    public MacroPropertyResolver(Resolvable baseResolver) {
        this.baseResolver = baseResolver;
    }

    @Override
    public String getExpandedValue(String unexpanded) {
        // TODO: This is very naive expansion. Rework this to be as robust as normal property expansion!!!
        String expanded = unexpanded;

        for (String attributeName: attributes) {
            // TODO: Use namingManager.getClassName here
            expanded = expanded.replace("@{"+attributeName+"}", "\"+this.get"+ namingManager.getClassNameFor(attributeName.toLowerCase())+"()+\"");
        }

        return baseResolver.getExpandedValue(expanded);
    }
}
