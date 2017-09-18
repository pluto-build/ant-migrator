package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.JavaGenerator;
import antplutomigrator.generate.NamingManager;
import antplutomigrator.generate.Resolvable;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.UnknownElement;

import java.util.Arrays;

public abstract class Transformer {
    protected Log log = LogFactory.getLog(this.getClass());

    protected final UnknownElement element;
    protected final ElementGenerator elementGenerator;
    protected final AntIntrospectionHelper introspectionHelper;
    protected final NamingManager namingManager;
    protected final Resolvable resolver;
    protected final JavaGenerator generator;
    protected final String taskName;

    public abstract boolean supportsElement();
    public Transformer(UnknownElement element, ElementGenerator elementGenerator, AntIntrospectionHelper introspectionHelper) {
        this.element = element;
        this.elementGenerator = elementGenerator;
        this.introspectionHelper = introspectionHelper;
        this.generator = elementGenerator.getGenerator();
        this.namingManager = elementGenerator.getNamingManager();
        this.resolver = elementGenerator.getResolver();
        this.taskName = namingManager.getNameFor(element);
    }

    public abstract void transform() throws RuntimeException;



    public boolean containsOnlySupportedAttributes(String... attr) {
        return Arrays.asList(attr).containsAll(element.getWrapper().getAttributeMap().keySet());
    }

    public boolean containsKey(String key) {
        return element.getWrapper().getAttributeMap().containsKey(key);
    }

    public String attributeForKey(String key) {
        Object attr = element.getWrapper().getAttributeMap().get(key);
        // TODO: Correct handling for null attributes
        if (attr == null)
            return null;
        return attr.toString();
    }

    public String expand(String str) {
        return resolver.getExpandedValue(str);
    }

    public String generateToBoolean(String value) {
        if (Arrays.asList("on", "off", "true", "false", "yes", "no").contains(value)) {
            return Project.toBoolean(value) + "";
        }
        return elementGenerator.getContextName()+".toBoolean(\""+StringEscapeUtils.escapeJava(value)+"\")";
    }
    public String generateToFile(String value) {
        return elementGenerator.getContextName()+".toFile(\""+StringEscapeUtils.escapeJava(value)+"\")";
    }
    public String generateToString(String value) {
        return elementGenerator.getContextName()+".toString(\""+ StringEscapeUtils.escapeJava(value)+"\")";
    }
}
