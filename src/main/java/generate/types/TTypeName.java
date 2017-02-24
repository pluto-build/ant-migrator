package generate.types;

/**
 * Created by manuel on 23.02.17.
 */
public class TTypeName {
    final private String fullyQualifiedName;

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getImportName() {
        // We have an inner class, import the parent...
        if (getFullyQualifiedName().contains("$")) {
            return getFullyQualifiedName().substring(0, getFullyQualifiedName().indexOf("$"));
        }
        // Otherwise return full canonical name...
        return getCanonicalName();
    }

    public String getShortName() {
        if (getFullyQualifiedName().contains(".")) {
            return getFullyQualifiedName().substring(getFullyQualifiedName().lastIndexOf(".")+1).replace("$", ".");
        }
        return getFullyQualifiedName();
    }

    public String getCanonicalName() {
        return getFullyQualifiedName().replace("$", ".");
    }

    public TTypeName(String name) {
        this.fullyQualifiedName = name;
    }
}
