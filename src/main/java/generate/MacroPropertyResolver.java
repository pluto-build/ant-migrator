package generate;

import org.apache.tools.ant.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 21.02.17.
 */
public class MacroPropertyResolver implements Resolvable {

    private final Resolvable baseResolver;
    private List<String> attributes = new ArrayList<>();

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
        String expanded = baseResolver.getExpandedValue(unexpanded);

        for (String attributeName: attributes) {
            expanded = expanded.replace("@{"+attributeName+"}", "\"+this."+attributeName+"+\"");
        }

        return expanded;
    }
}
