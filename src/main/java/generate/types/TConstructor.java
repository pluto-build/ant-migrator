package generate.types;

import generate.anthelpers.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 23.02.17.
 */
public class TConstructor {

    private final String name;
    private final TTypeName declaringClassStringTypeName;
    private final List<TParameter> parameters;

    public String getName() {
        return name;
    }
    public List<TParameter> getParameters() {
        return parameters;
    }

    public TTypeName getDeclaringClassTypeName() {
        return declaringClassStringTypeName;
    }
    public Class<?> getOnClass() {
        return ReflectionHelpers.getClassFor(declaringClassStringTypeName.getFullyQualifiedName());
    }

    public TConstructor(String name, List<TParameter> parameters, TTypeName declaringClassStringTypeName) {
        this.name = name;
        this.parameters = parameters;
        this.declaringClassStringTypeName = declaringClassStringTypeName;
    }

    public TConstructor(java.lang.reflect.Constructor<?> constructor) {
        this.name = constructor.getName();
        this.declaringClassStringTypeName = new TTypeName(constructor.getDeclaringClass().getName());
        this.parameters = new ArrayList<>();
        for (java.lang.reflect.Parameter parameter: constructor.getParameters()) {
            parameters.add(new TParameter(parameter.getName(), new TTypeName(parameter.getType().getName())));
        }
    }

    public String format() {
        String p = "(";

        for (TParameter parameter: this.getParameters()) {
            p += parameter.getTypeString().getFullyQualifiedName() + " " + parameter.getName() + ", ";
        }

        if (p.endsWith(", "))
            p = p.substring(0, p.length()-2);

        return this.getName() + p + ")";
    }

    public String formatUse(List<String> parameterNames) {
        String p = "(";

        for (String parameter: parameterNames) {
            p += parameter + ", ";
        }

        if (p.endsWith(", "))
            p = p.substring(0, p.length()-2);

        return this.getName() + p + ")";
    }
}
