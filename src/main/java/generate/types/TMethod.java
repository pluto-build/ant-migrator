package generate.types;

import generate.anthelpers.ReflectionHelpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by manuel on 23.02.17.
 */
public class TMethod {

    private final String name;
    private final List<TParameter> parameters;
    private final TTypeName returnTypeString;

    public String getName() {
        return name;
    }
    public List<TParameter> getParameters() {
        return parameters;
    }

    @Deprecated
    public Class<?> getReturnType() {
        return ReflectionHelpers.getClassFor(returnTypeString.getFullyQualifiedName());
    }

    public TTypeName getReturnTypeName() {
        return returnTypeString;
    }

    public TMethod(String name, List<TParameter> parameters, TTypeName returnTypeString) {
        this.name = name;
        this.parameters = parameters;
        this.returnTypeString = returnTypeString;
    }

    public TMethod(Method method) {
        assert(method != null);
        this.name = method.getName();
        this.parameters = new ArrayList<>();
        for (java.lang.reflect.Parameter parameter: method.getParameters()) {
            parameters.add(new TParameter(parameter.getName(), new TTypeName(parameter.getType().getName())));
        }
        this.returnTypeString = new TTypeName(method.getReturnType().getName());
    }
}
