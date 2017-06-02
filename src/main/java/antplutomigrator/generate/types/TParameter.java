package antplutomigrator.generate.types;

import antplutomigrator.generate.anthelpers.ReflectionHelpers;

import java.lang.reflect.Parameter;

/**
 * Created by manuel on 23.02.17.
 */
public class TParameter {

    private final String name;
    private final TTypeName typeName;


    public String getName() {
        return name;
    }
    public TTypeName getTypeName() {
        return typeName;
    }

    public Class<?> getType() {
        return ReflectionHelpers.getClassFor(typeName.getFullyQualifiedName());
    }


    public TParameter(String name, TTypeName typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public TParameter(Parameter parameter) {
        this.name = parameter.getName();
        this.typeName = new TTypeName(parameter.getType().getName());
    }
}
