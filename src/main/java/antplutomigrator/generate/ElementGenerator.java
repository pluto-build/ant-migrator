package antplutomigrator.generate;

import antplutomigrator.generate.anthelpers.ReflectionHelpers;
import antplutomigrator.generate.introspectionhelpers.AntIntrospectionHelper;
import antplutomigrator.generate.transformers.Transformer;
import antplutomigrator.generate.transformers.TransformerFactory;
import antplutomigrator.generate.types.TConstructor;
import antplutomigrator.generate.types.TMethod;
import antplutomigrator.generate.types.TParameter;
import antplutomigrator.generate.types.TTypeName;
import javafx.util.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.EnumeratedAttribute;
import antplutomigrator.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by manuel on 16.02.17.
 */
public class ElementGenerator {

    private Log log = LogFactory.getLog(ElementGenerator.class);

    private final JavaGenerator generator;
    private final Project project;
    private final NamingManager namingManager;
    private final Resolvable resolver;
    private List<String> ignoredMacroElements = new ArrayList<>();
    private Map<UnknownElement, Pair<String, TTypeName>> constructedVariables = new HashMap<>();
    private boolean localScopedVariables = true;
    private boolean noConstructor = false;
    private boolean onlyConstructors = false;
    private final boolean continueOnErrors;
    private final String contextName;
    private boolean inMacro = false;

    public List<String> getIgnoredMacroElements() {
        return ignoredMacroElements;
    }

    public void setIgnoredMacroElements(List<String> ignoredMacroElements) {
        this.ignoredMacroElements = ignoredMacroElements;
    }

    public Map<UnknownElement, Pair<String, TTypeName>> getConstructedVariables() {
        return constructedVariables;
    }
    public void setLocalScopedVariables(boolean localScopedVariables) {
        this.localScopedVariables = localScopedVariables;
    }

    public void setNoConstructor(boolean noConstructor) {
        this.noConstructor = noConstructor;
    }

    public void setOnlyConstructors(boolean onlyConstructors) {
        this.onlyConstructors = onlyConstructors;
    }

    public NamingManager getNamingManager() {
        return namingManager;
    }

    public String getProjectName() {
        return getNamingManager().getClassNameFor(StringUtils.capitalize(project.getName()));
    }

    public boolean isInMacro() {
        return inMacro;
    }

    public void setInMacro(boolean inMacro) {
        this.inMacro = inMacro;
    }

    public String getInputName() {
        return getProjectName() + "Context";
    }

    public JavaGenerator getGenerator() {
        return generator;
    }

    public Resolvable getResolver() {
        return resolver;
    }

    public boolean isLocalScopedVariables() {
        return localScopedVariables;
    }

    public boolean isNoConstructor() {
        return noConstructor;
    }

    public boolean isOnlyConstructors() {
        return onlyConstructors;
    }

    public boolean isContinueOnErrors() {
        return continueOnErrors;
    }

    public String getContextName() {
        return contextName;
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, Resolvable resolver, boolean continueOnErrors) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.continueOnErrors = continueOnErrors;
        this.contextName = "context";
    }

    public ElementGenerator(JavaGenerator generator, Project project, NamingManager namingManager, Resolvable resolver, boolean continueOnErrors, String contextName) {
        this.generator = generator;
        this.project = project;
        this.namingManager = namingManager;
        this.resolver = resolver;
        this.continueOnErrors = continueOnErrors;
        this.contextName = contextName;
    }

    public String generateElement(AntIntrospectionHelper parentIntrospectionHelper, UnknownElement element, String taskName) {
        return generateElement(parentIntrospectionHelper, element, taskName, false);
    }

    public String generateElement(AntIntrospectionHelper parentIntrospectionHelper, UnknownElement element, String taskName, boolean implicitInserted)  {
        try {
            log.trace("Generating element: " + element.getTaskName() + " at " + element.getLocation().toString());
            Statistics.getInstance().generatedElement(element);

            // Search for implicit elements and insert them explicitly...
            if (!implicitInserted)
                insertImplicitElements(element, parentIntrospectionHelper);

            // macros were already migrated...
            if (element.getTaskName().equals("macrodef"))
                return taskName;

            if (element.getTaskName().equals("taskdef") || element.getTaskName().equals("componentdef") || element.getTaskName().equals("typedef")) {
                log.error("Encountered " + element.getTaskName() + " at " + element.getLocation() + ". This is not yet supported...");
                return taskName;
            }

            if (taskName == null)
                taskName = getNamingManager().getNameFor(element);

            if (ignoredMacroElements.contains(element.getTaskName())) {
                if (element.getChildren() == null || element.getChildren().isEmpty())
                    return taskName;
            }

            AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, taskName, generator.getPkg(), parentIntrospectionHelper);

            Transformer transformer = TransformerFactory.getTransformer(element, this, introspectionHelper);
            transformer.transform();

            return taskName;
        } catch (RuntimeException e) {
            if (!continueOnErrors)
                throw e;
            log.error("Failed generating element: " + element.getTaskName() + " at " + element.getLocation().toString(), e);
            generator.printString("// TODO: Error while migrating " + element.getTaskName() + " at " + element.getLocation().toString());
            if (element.getChildren() != null) {
                generator.printString("// all children will also not be generated...");
                for (UnknownElement c: element.getChildren()) {
                    log.error("-->  Failed generating child: " + c.getTaskName() + " at " + c.getLocation().toString());
                    generator.printString("//  -->  Failed generating element: " + c.getTaskName() + " at " + c.getLocation().toString());
                }
            }
        }
        return taskName;
    }

    public void insertImplicitElements(UnknownElement element, AntIntrospectionHelper parentIntrospectionHelper) {
        AntIntrospectionHelper introspectionHelper = AntIntrospectionHelper.getInstanceFor(project, element, element.getTaskName(), generator.getPkg(), parentIntrospectionHelper);

        if (introspectionHelper.hasImplicitElement()) {

            log.debug("Inserting implicit element " + introspectionHelper.getImplicitElementName() + " into " + element.getTaskName() + " at " + element.getLocation());
            UnknownElement implicitElement = new UnknownElement(introspectionHelper.getImplicitElementName());
            implicitElement.setTaskName(introspectionHelper.getImplicitElementName());
            implicitElement.setRuntimeConfigurableWrapper(new RuntimeConfigurable(implicitElement, introspectionHelper.getImplicitElementName()));
            implicitElement.setLocation(element.getLocation());
            for (UnknownElement child: element.getChildren()) {
                insertImplicitElements(child, introspectionHelper);
                implicitElement.addChild(child);
            }

            ReflectionHelpers.clearChildrenFor(element);
            element.addChild(implicitElement);
        }
    }

    public String getProject() {
        if (this.contextName.equals("this")) {
            return "project";
        }
        else return this.contextName + ".project()";
    }
}
