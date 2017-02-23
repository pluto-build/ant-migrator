package generate.types;

/**
 * Created by manuel on 23.02.17.
 */
public class TTypeName {
    final private String fullyQualifiedName;

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getShortName() {
        if (getFullyQualifiedName().contains(".")) {
            return getFullyQualifiedName().substring(getFullyQualifiedName().lastIndexOf(".")+1);
        }
        return fullyQualifiedName;
    }

    public TTypeName(String name) {
        this.fullyQualifiedName = name;
    }
}
