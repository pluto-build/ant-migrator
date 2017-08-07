package <pkg>;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class AntBuilder extends Builder<<ctx>, <ctx>> {

    private String name;
    private final int hashCode;

    public AntBuilder(<ctx> context) {
        super(context);
        this.name = this.getClass().getSimpleName();
        if (context.getBuilderName().equals("antcall"))
            this.hashCode = context.hashCode();
        else
            this.hashCode = 0;
    }

    @Override
    public <In_ extends Serializable, Out_ extends Output, B_ extends Builder<In_, Out_>, F_ extends BuilderFactory<In_, Out_, B_>, SubIn_ extends In_> Out_ requireBuild(F_ factory, SubIn_ context) throws IOException {
        assert context instanceof <ctx>;
        if (((<ctx>)context).getBuilderName().equals("antcall"))
            return super.requireBuild(factory, (In_)((<ctx>)context).withName(((<ctx>)context).getBuilderName()));
        return super.requireBuild(factory, (In_)((<ctx>)context).withName(name));
    }

    public <In_ extends <ctx>, Out_ extends <ctx>, B_ extends Builder<In_, Out_>, F_ extends BuilderFactory<In_, Out_, B_>, SubIn_ extends In_> void antCall(F_ factory,  SubIn_ context) {
        try {
            super.requireBuild(factory, (In_)context.withName("antcall"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String description(<ctx> context) {
        return "Builder "+name+": " + context;
    }

    @Override
    public File persistentPath(<ctx> context) {
        return new File("deps/"+name+"."+hashCode+".dep");
    }

    @Override
    protected Stamper defaultStamper() {
        return LastModifiedStamper.instance;
    }

    <fd>
}
