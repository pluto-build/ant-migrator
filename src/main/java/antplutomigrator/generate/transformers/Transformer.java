package antplutomigrator.generate.transformers;

import antplutomigrator.generate.ElementGenerator;
import antplutomigrator.generate.JavaGenerator;
import antplutomigrator.generate.NamingManager;
import antplutomigrator.generate.Resolvable;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.UnknownElement;

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
}
