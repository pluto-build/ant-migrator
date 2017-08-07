package antplutomigrator.generate.types;

import antplutomigrator.generate.anthelpers.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 23.02.17.
 */
public class TConstructor {

    private TTypeName name;
    private TTypeName declaringClassStringTypeName;
    private List<TParameter> parameters;

    public TTypeName getName() {
        return name;
    }
    public List<TParameter> getParameters() {
        return parameters;
    }

    public TTypeName getDeclaringClassTypeName() {
        return declaringClassStringTypeName;
    }

    public void setName(TTypeName name) {
        this.name = name;
    }

    public void setDeclaringClassStringTypeName(TTypeName declaringClassStringTypeName) {
        this.declaringClassStringTypeName = declaringClassStringTypeName;
    }

    public void setParameters(List<TParameter> parameters) {
        this.parameters = parameters;
    }

    public Class<?> getOnClass() {
        return ReflectionHelpers.getClassFor(declaringClassStringTypeName.getFullyQualifiedName());
    }

    public TConstructor(TTypeName name, List<TParameter> parameters, TTypeName declaringClassStringTypeName) {
        this.name = name;
        this.parameters = parameters;
        this.declaringClassStringTypeName = declaringClassStringTypeName;
    }

    public TConstructor(java.lang.reflect.Constructor<?> constructor) {
        this.name = new TTypeName(constructor.getName());
        this.declaringClassStringTypeName = new TTypeName(constructor.getDeclaringClass().getName());
        this.parameters = new ArrayList<>();
        for (java.lang.reflect.Parameter parameter: constructor.getParameters()) {
            parameters.add(new TParameter(parameter.getName(), new TTypeName(parameter.getType().getName())));
        }
    }

    public String format() {
        String p = "(";

        for (TParameter parameter: this.getParameters()) {
            p += parameter.getTypeName().getFullyQualifiedName() + " " + parameter.getName() + ", ";
        }

        if (p.endsWith(", "))
            p = p.substring(0, p.length()-2);

        return this.getName().getShortName() + p + ")";
    }

    public String formatUse(List<String> parameterNames) {
        String p = "(";

        for (String parameter: parameterNames) {
            p += parameter + ", ";
        }

        if (p.endsWith(", "))
            p = p.substring(0, p.length()-2);

        return this.getName().getShortName() + p + ")";
    }
}
