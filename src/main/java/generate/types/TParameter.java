package generate.types;

import generate.anthelpers.ReflectionHelpers;

import java.lang.reflect.Parameter;

/**
 * Created by manuel on 23.02.17.
 */
public class TParameter {

    private final String name;
    private final TTypeName typeString;


    public String getName() {
        return name;
    }
    public TTypeName getTypeString() {
        return typeString;
    }

    public Class<?> getType() {
        return ReflectionHelpers.getClassFor(typeString.getFullyQualifiedName());
    }


    public TParameter(String name, TTypeName typeString) {
        this.name = name;
        this.typeString = typeString;
    }

    public TParameter(Parameter parameter) {
        this.name = parameter.getName();
        this.typeString = new TTypeName(parameter.getType().getName());
    }
}
